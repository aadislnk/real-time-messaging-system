package com.aadi.sandesh.config;

import com.aadi.sandesh.security.ChatPrincipal;
import com.aadi.sandesh.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WebSocketPresenceListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketPresenceListener.class);
    private final PresenceService presenceService;

    private final ConcurrentHashMap<String, UUID> sessionUsers = new ConcurrentHashMap<>();

    @EventListener
    public void connected(SessionConnectedEvent event) {
        var accessor = StompHeaderAccessor.wrap(event.getMessage());

        if (accessor.getUser() instanceof ChatPrincipal principal && accessor.getSessionId() != null) {
            var sessionId = accessor.getSessionId();
            var userId = principal.userId();

            sessionUsers.put(sessionId, userId);
            presenceService.connected(userId, sessionId);
            logger.debug("WebSocket session connected: userId={}, sessionId={}", userId, sessionId);
        }
    }

    @EventListener
    public void disconnected(SessionDisconnectEvent event) {
        var accessor = StompHeaderAccessor.wrap(event.getMessage());
        var sessionId = event.getSessionId();

        UUID userId = null;
        if (accessor.getUser() instanceof ChatPrincipal principal) {
            userId = principal.userId();
        }
        if (userId == null && sessionId != null) {
            userId = sessionUsers.remove(sessionId);
        } else if (sessionId != null) {
            sessionUsers.remove(sessionId);
        }

        if (userId != null && sessionId != null) {
            presenceService.disconnected(userId, sessionId);
            logger.debug("WebSocket session disconnected: userId={}, sessionId={}", userId, sessionId);
        }
    }
}
