package com.aadi.sandesh.dto.user;


import java.util.UUID;

public record UserResponse(
        UUID id,
        String displayName,
        String email
) {
}
