package com.aadi.sandesh.controller;

import com.aadi.sandesh.dto.presence.PresenceResponse;
import com.aadi.sandesh.security.CurrentUser;
import com.aadi.sandesh.service.MembershipService;
import com.aadi.sandesh.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
        "/api/conversations/{conversationId}/presence"
)
@RequiredArgsConstructor
public class PresenceController {

    private final MembershipService membershipService;

    private final CurrentUser currentUser;

    private final PresenceService presenceService;

    @GetMapping("/{userId}")
    public PresenceResponse get(
            Authentication authentication,
            @PathVariable UUID conversationId,
            @PathVariable UUID userId
    ) {
        membershipService.requireMember(conversationId, currentUser.id(authentication));

        membershipService.requireMember(
                conversationId, userId
        );

        return presenceService.get(userId);
    }

    @GetMapping
    public List<PresenceResponse> list(
            Authentication authentication,
            @PathVariable UUID conversationId
    ) {
        membershipService.requireMember(conversationId, currentUser.id(authentication));

        return presenceService.list(conversationId);
    }
}