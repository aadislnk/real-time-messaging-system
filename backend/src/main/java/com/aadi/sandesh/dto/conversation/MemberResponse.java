package com.aadi.sandesh.dto.conversation;

import com.aadi.sandesh.domain.enums.MemberRole;

import java.util.UUID;

public record MemberResponse(
        UUID userId,
        String displayName,
        MemberRole role,
        long lastReadSequence
) {
}