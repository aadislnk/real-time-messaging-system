package com.aadi.sandesh.dto.message;

import java.util.List;

public record MessagePageResponse(
        List<MessageResponse> messages,
        Long nextAfterSequence,
        boolean hasMore
) {
}