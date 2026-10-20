package com.aadi.sandesh.security;

import com.aadi.sandesh.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUser {
    public UUID id(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof ChatPrincipal principal)) {
            throw new UnauthorizedException("Authentication required.");
        }

        return principal.userId();
    }
}
