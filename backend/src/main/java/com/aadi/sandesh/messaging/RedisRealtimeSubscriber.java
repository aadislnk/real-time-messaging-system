package com.aadi.sandesh.messaging;

import com.aadi.sandesh.dto.event.RealtimeEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class RedisRealtimeSubscriber implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisRealtimeSubscriber.class);

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ObjectMapper objectMapper;
    private final Optional<MeterRegistry> meterRegistry;

    public RedisRealtimeSubscriber(
            SimpMessagingTemplate simpMessagingTemplate,
            ObjectMapper objectMapper,
            @Autowired(required = false) MeterRegistry meterRegistry
    ) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = Optional.ofNullable(meterRegistry);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            var event = objectMapper.readValue(payload, RealtimeEvent.class);

            if (event.targetUserId() == null) {
                logger.warn("Received realtime event with null targetUserId; ignoring");
                return;
            }

            simpMessagingTemplate.convertAndSendToUser(
                    event.targetUserId().toString(),
                    "/queue/events",
                    event
            );
            meterRegistry.ifPresent(m -> m.counter("sandesh.realtime.delivered").increment());
            logger.debug("Delivered realtime event {} to user {}", event.type(), event.targetUserId());
        } catch (JacksonException exception) {
            logger.error("Failed to deserialize realtime event message: {}", exception.getMessage());
            meterRegistry.ifPresent(m -> m.counter("sandesh.realtime.deserialization_errors").increment());
        } catch (Exception ex) {
            logger.error("Unexpected error in realtime subscriber: {}", ex.getMessage(), ex);
        }
    }
}
