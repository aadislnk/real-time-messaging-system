package com.aadi.sandesh.dto.conversation;

import com.aadi.sandesh.domain.enums.MemberRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddMemberRequest(
        @NotNull UUID userId,
        @NotNull MemberRole role
) {
}