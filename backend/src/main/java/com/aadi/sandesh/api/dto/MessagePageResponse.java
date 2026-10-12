package com.aadi.sandesh.api.dto;

import java.util.List;

public record MessagePageResponse(
        List<MessageResponse> messages,
        Long nextAfterSequence,
        boolean hasMore
) {
}