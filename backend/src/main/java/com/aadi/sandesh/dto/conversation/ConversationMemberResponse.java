package com.aadi.sandesh.dto.conversation;

import com.aadi.sandesh.domain.enums.MemberRole;

import java.util.UUID;

public record ConversationMemberResponse(
        UUID conversationId,
        UUID userId,
        String displayName,
        MemberRole role,
        long lastReadSequence,
        Long hiddenAfterSequence
) {
}