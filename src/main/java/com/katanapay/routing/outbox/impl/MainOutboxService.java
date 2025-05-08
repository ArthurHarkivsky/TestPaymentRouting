package com.katanapay.routing.outbox.impl;

import com.katanapay.routing.model.entity.OutboxEvent;
import com.katanapay.routing.outbox.OutboxService;
import com.katanapay.routing.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MainOutboxService implements OutboxService {

    private final OutboxEventRepository outboxEventRepository;

    @Override
    @Transactional
    public OutboxEvent createOutboxEvent(String aggregateType, String aggregateId, String eventType, String payload) {
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .attemptCount(0)
                .processed(false)
                .locked(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return outboxEventRepository.save(outboxEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> getUnprocessedEvents(int limit) {
        return outboxEventRepository.findUnprocessedEvents(limit);
    }

    @Override
    @Transactional
    public void markAsProcessed(UUID id) {
        OutboxEvent event = outboxEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Outbox event not found: " + id));

        event.setProcessed(true);
        event.setLocked(false);
        event.setUpdatedAt(LocalDateTime.now());

        outboxEventRepository.save(event);
        log.debug("Marked outbox event as processed: {}", id);
    }

    @Override
    @Transactional
    public void updateAttemptCount(UUID id, int attemptCount) {
        OutboxEvent event = outboxEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Outbox event not found: " + id));

        event.setAttemptCount(attemptCount);
        event.setUpdatedAt(LocalDateTime.now());

        outboxEventRepository.save(event);
        log.debug("Updated outbox event attempt count: {}, count: {}", id, attemptCount);
    }

    @Override
    @Transactional
    public boolean acquireLock(UUID id) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lockExpiry = now.plusMinutes(5); // todo move to props

        int updatedRows = outboxEventRepository.acquireLock(id, lockExpiry, now);

        boolean acquired = updatedRows > 0;
        if (acquired) {
            log.debug("Acquired lock on outbox event: {}", id);
        }

        return acquired;
    }

    @Override
    @Transactional
    public void releaseLock(UUID id) {
        OutboxEvent event = outboxEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Outbox event not found: " + id));

        event.setLocked(false);
        event.setLockExpiry(null);
        event.setUpdatedAt(LocalDateTime.now());

        outboxEventRepository.save(event);
        log.debug("Released lock on outbox event: {}", id);
    }
}