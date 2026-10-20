package com.aadi.sandesh.controller;

import com.aadi.sandesh.domain.enums.EventType;
import com.aadi.sandesh.dto.event.RealtimeEvent;
import com.aadi.sandesh.messaging.RedisRealtimePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@Profile({"dev", "test"})
@RestController
@RequestMapping("/test/realtime")
@RequiredArgsConstructor
public class RealtimeTestController {

    private final RedisRealtimePublisher publisher;

    @PostMapping("/{userId}")
    public void test(@PathVariable UUID userId) {
        var event = new RealtimeEvent(
                EventType.TEST,
                userId,
                null,
                null,
                null,
                "Hello from Redis",
                Instant.now()
        );
        publisher.publish(event);
    }
}
