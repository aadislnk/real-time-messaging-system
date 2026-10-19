package com.aadi.sandesh.dto.message;

import java.time.Instant;
import java.util.UUID;

public record MessageSearchResultResponse(
        UUID messageId,
        UUID conversationId,
        UUID senderId,
        String content,
        Instant createdAt,
        long sequenceNumber
) {
}