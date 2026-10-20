package com.aadi.sandesh.repository;

import com.aadi.sandesh.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    @Transactional
    @Modifying
    @Query(value = """
            INSERT into processed_events (event_id, processed_at)
                    VALUES (:eventId, :processedAt)
                            ON CONFLICT (event_id) DO NOTHING 
            """, nativeQuery = true)
    int markProcessed(
            @Param("eventId") UUID eventId,
            @Param("processedAt") Instant processedAt
    );
}
