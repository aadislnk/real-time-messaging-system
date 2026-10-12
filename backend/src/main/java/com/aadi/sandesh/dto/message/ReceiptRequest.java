package com.aadi.sandesh.dto.message;


import com.aadi.sandesh.domain.enums.ReceiptType;
import jakarta.validation.constraints.NotNull;

public record ReceiptRequest(
        @NotNull ReceiptType type
) {
}
