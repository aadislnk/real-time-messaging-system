package com.aadi.sandesh.dto.conversation;

import jakarta.validation.constraints.Size;

public record UpdateConversationRequest(
        @Size(max = 120) String title
) {
}