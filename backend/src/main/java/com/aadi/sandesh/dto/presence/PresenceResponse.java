package com.aadi.sandesh.dto.presence;

import java.time.Instant;
import java.util.UUID;

public record PresenceResponse(
        UUID userId,
        boolean online,
        Instant lastSeenAt
) {
}