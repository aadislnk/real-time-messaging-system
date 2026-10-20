package com.aadi.sandesh.dto.event;

import com.aadi.sandesh.domain.enums.EventType;

import java.time.Instant;
import java.util.UUID;

public record RealtimeEvent(
        EventType type,
        UUID targetUserId,
        UUID conversationId,
        UUID actorUserId,
        UUID messageId,
        Object payload,
        Instant occurredAt
) {
}