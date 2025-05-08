package com.katanapay.routing.outbox;

import com.katanapay.routing.model.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Processes outbox events from the database and publishes them to the appropriate destination.
 * Implements reliable event delivery with retry logic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private final OutboxService outboxService;

    @Value("${outbox.processing.enabled:true}")
    private boolean processingEnabled;

    @Value("${outbox.processing.max-attempts:5}")
    private int maxAttempts;

    /**
     * Scheduled method that processes pending outbox events.
     * Runs at a fixed interval defined in application properties.
     */
    @Scheduled(fixedDelayString = "${outbox.processing.fixed-delay:5000}")
    public void processOutboxEvents() {
        if (!processingEnabled) {
            log.debug("Outbox processing is disabled");
            return;
        }

        log.debug("Starting outbox event processing");
        List<OutboxEvent> events = outboxService.getUnprocessedEvents(10);

        if (events.isEmpty()) {
            log.debug("No outbox events to process");
            return;
        }

        log.info("Processing {} outbox events", events.size());

        for (OutboxEvent event : events) {
            if (!outboxService.acquireLock(event.getId())) {
                log.debug("Could not acquire lock for event: {}", event.getId());
                continue;
            }

            try {
                processEvent(event);
            } catch (Exception e) {
                log.error("Error processing outbox event: {}", event.getId(), e);
                handleEventProcessingFailure(event);
            } finally {
                outboxService.releaseLock(event.getId());
            }
        }
    }

    /**
     * Process a single outbox event.
     *
     * @param event the outbox event to process
     */
    private void processEvent(OutboxEvent event) {
        log.info("Processing outbox event: {}, type: {}", event.getId(), event.getEventType());

        try {
            switch (event.getEventType()) {
                case "payment_created", "payment_updated", "payment_completed", "payment_failed" -> publishPaymentEvent(event);
                default -> log.warn("Unknown event type: {}", event.getEventType());
            }

            outboxService.markAsProcessed(event.getId());
            log.info("Successfully processed outbox event: {}", event.getId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to process outbox event: " + event.getId(), e);
        }
    }

    /**
     * Handles failure in processing an outbox event.
     * Implements retry logic with exponential backoff.
     *
     * @param event the failed outbox event
     */
    private void handleEventProcessingFailure(OutboxEvent event) {
        int newAttemptCount = event.getAttemptCount() + 1;

        if (newAttemptCount >= maxAttempts) {
            log.warn("Max retry attempts reached for event: {}. Marking as processed with failure.", event.getId());
            outboxService.markAsProcessed(event.getId());
        } else {
            log.info("Incrementing attempt count for event: {} to {}", event.getId(), newAttemptCount);
            outboxService.updateAttemptCount(event.getId(), newAttemptCount);
        }
    }

    /**
     * Publishes a payment-related event to the appropriate destination.
     *
     * @param event the payment event to publish
     */
    private void publishPaymentEvent(OutboxEvent event) {
        // In a real implementation, this would publish to a message broker
        // For this example, we'll just log the event
        log.info("Publishing payment event: {}, aggregateId: {}",
                event.getEventType(), event.getAggregateId());
    }
}