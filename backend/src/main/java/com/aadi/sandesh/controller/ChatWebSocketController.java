package com.aadi.sandesh.controller;

import com.aadi.sandesh.dto.event.TypingEvent;
import com.aadi.sandesh.security.ChatPrincipal;
import com.aadi.sandesh.service.PresenceService;
import com.aadi.sandesh.service.TypingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {
    private final PresenceService presenceService;
    private final TypingService typingService;

    @MessageMapping("/presence/heartbeat")
    public void heartbeat(
            Principal principal,
            StompHeaderAccessor accessor
    ) {
        presenceService.heartbeat(
                userId(principal),
                accessor.getSessionId()
        );
    }

    @MessageMapping("/conversations/{conversationId}/typing")
    public void typing(
            Principal principal,
            @DestinationVariable UUID conversationId,
            @Valid TypingEvent event
    ) {
        typingService.update(
                userId(principal),
                conversationId,
                event.typing()
        );
    }

    private UUID userId(Principal principal) {
        return ((ChatPrincipal) principal).userId();
    }
}