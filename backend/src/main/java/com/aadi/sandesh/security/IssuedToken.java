package com.aadi.sandesh.security;

import java.time.Instant;

public record IssuedToken(String value, Instant expiresAt) {
}
