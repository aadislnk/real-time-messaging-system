package com.aadi.sandesh.api.dto;

import com.aadi.sandesh.domain.MemberRole;

import java.util.UUID;

public record MemberResponse(
        UUID userId,
        String displayName,
        MemberRole role,
        long lastReadSequence
) {
}