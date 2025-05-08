package com.katanapay.routing.outbox;

import com.katanapay.routing.model.entity.OutboxEvent;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing outbox events.
 * Implements the transactional outbox pattern for reliable event publishing.
 */
public interface OutboxService {

    /**
     * Saves a new outbox event.
     *
     * @param aggregateType the type of the aggregate (e.g., "payment")
     * @param aggregateId   the ID of the aggregate
     * @param eventType     the type of the event (e.g., "payment_created")
     * @param payload       the JSON payload of the event
     * @return the created outbox event
     */
    OutboxEvent createOutboxEvent(String aggregateType, String aggregateId, String eventType, String payload);

    /**
     * Retrieves a list of unprocessed outbox events that are ready for processing.
     *
     * @param limit the maximum number of events to retrieve
     * @return list of outbox events
     */
    List<OutboxEvent> getUnprocessedEvents(int limit);

    /**
     * Marks an outbox event as processed.
     *
     * @param id the ID of the outbox event
     */
    void markAsProcessed(UUID id);

    /**
     * Updates the attempt count for an outbox event.
     *
     * @param id           the ID of the outbox event
     * @param attemptCount the new attempt count
     */
    void updateAttemptCount(UUID id, int attemptCount);

    /**
     * Acquires a lock on an outbox event for processing.
     *
     * @param id the ID of the outbox event
     * @return true if the lock was acquired, false otherwise
     */
    boolean acquireLock(UUID id);

    /**
     * Releases a lock on an outbox event.
     *
     * @param id the ID of the outbox event
     */
    void releaseLock(UUID id);
}