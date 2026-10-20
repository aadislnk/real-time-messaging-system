package com.aadi.sandesh.domain;

import com.aadi.sandesh.domain.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 40)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    public OutboxEvent(
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String payloadJson
    ) {
        this.id = UUID.randomUUID();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payloadJson = payloadJson;
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
        this.availableAt = Instant.now();
        this.createdAt = Instant.now();
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        this.lastError = null;
    }

    public void scheduleRetry() {
        scheduleRetry(null);
    }

    public void scheduleRetry(String error) {
        this.attempts++;
        this.lastError = sanitizeError(error);
        this.status = attempts >= 10 ? OutboxStatus.FAILED : OutboxStatus.PENDING;

        // Exponential backoff: 2s, 4s, 8s, 16s, ... capped at 300s
        long delaySeconds = Math.min(300L, 1L << Math.min(attempts, 8));
        this.availableAt = Instant.now().plusSeconds(delaySeconds);
    }

    public void markFailed(String error) {
        this.status = OutboxStatus.FAILED;
        this.lastError = sanitizeError(error);
    }

    private String sanitizeError(String error) {
        if (error == null || error.isBlank()) {
            return null;
        }
        return error.length() > 1000 ? error.substring(0, 1000) : error;
    }
}
