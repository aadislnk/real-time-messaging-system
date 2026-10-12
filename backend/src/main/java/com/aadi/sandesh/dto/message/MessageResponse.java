package com.aadi.sandesh.dto.message;


import com.aadi.sandesh.domain.enums.MessageType;
import com.aadi.sandesh.domain.enums.ReceiptState;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String clientMessageId,
        long sequenceNumber,
        MessageType type,
        String content,
        Instant createdAt,
        ReceiptState receiptState
) {
}
