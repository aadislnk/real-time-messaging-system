package com.aadi.sandesh.dto.conversation;

import com.aadi.sandesh.domain.enums.ConversationType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        ConversationType type,
        String title,
        UUID createdBy,
        Instant createdAt,
        List<MemberResponse> members,
        String latestMessagePreview
) {
}