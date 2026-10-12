package com.aadi.sandesh.dto.message;

import com.aadi.sandesh.domain.enums.ReceiptState;

import java.time.Instant;
import java.util.UUID;

public record ReceiptResponse(
        UUID messageId,
        UUID userId,
        Instant deliveredAt,
        Instant readAt,
        ReceiptState receiptState
) {
}