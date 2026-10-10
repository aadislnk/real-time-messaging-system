package com.aadi.sandesh.api.dto;

import jakarta.validation.constraints.Size;

public record UpdateConversationRequest(
        @Size(max = 120) String title
) {
}