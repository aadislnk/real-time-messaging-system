package com.aadi.sandesh.security;

import com.aadi.sandesh.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.nio.file.attribute.UserPrincipal;
import java.util.UUID;

@Component
public class CurrentUser {
    public UUID id(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof ChatPrincipal principal)) {
            throw new ForbiddenException("Authentication required.");
        }

        return principal.userId();
    }
}
