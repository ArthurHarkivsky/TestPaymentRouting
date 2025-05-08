package com.katanapay.routing.repository;

import com.katanapay.routing.model.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("""
            SELECT o
            FROM OutboxEvent o
            WHERE o.processed = false
                AND (o.locked = false
                            OR o.lockExpiry < NOW())
            ORDER BY o.createdAt ASC
            LIMIT :limit
            """)
    List<OutboxEvent> findUnprocessedEvents(@Param("limit") int limit);

    @Modifying
    @Transactional
    @Query("""
            UPDATE OutboxEvent o
            SET o.locked = true,
                o.lockExpiry = :lockExpiry,
                o.updatedAt = :now
            WHERE o.id = :id
              AND (o.locked = false OR o.lockExpiry < :now)
            """)
    int acquireLock(@Param("id") UUID id,
                    @Param("lockExpiry") LocalDateTime lockExpiry,
                    @Param("now") LocalDateTime now);
}
