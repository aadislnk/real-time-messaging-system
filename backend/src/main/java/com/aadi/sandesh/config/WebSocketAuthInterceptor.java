package com.aadi.sandesh.config;

import com.aadi.sandesh.security.ChatPrincipal;
import com.aadi.sandesh.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);
    private final JwtService jwtService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        var accessor = MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
        );

        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        var authorization = accessor.getFirstNativeHeader(
                HttpHeaders.AUTHORIZATION
        );

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            logger.warn("WebSocket CONNECT rejected: Missing or invalid Authorization header format");
            throw new IllegalArgumentException(
                    "WebSocket Authorization header is required"
            );
        }

        try {
            var principal = jwtService.parse(authorization.substring(7));
            accessor.setUser(principal);
            return message;
        } catch (Exception ex) {
            logger.warn("WebSocket CONNECT rejected: Invalid or expired token");
            throw new IllegalArgumentException(
                    "Invalid or expired authentication token"
            );
        }
    }
}
