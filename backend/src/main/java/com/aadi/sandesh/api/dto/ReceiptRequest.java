package com.aadi.sandesh.api.dto;


import com.aadi.sandesh.domain.enums.ReceiptType;
import jakarta.validation.constraints.NotNull;

public record ReceiptRequest(
        @NotNull ReceiptType type
) {
}
