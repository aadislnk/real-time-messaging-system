package com.aadi.sandesh.messaging;

import com.aadi.sandesh.domain.enums.OutboxStatus;
import com.aadi.sandesh.repository.OutboxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
public class OutboxPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final StringRedisTemplate redisTemplate;
    private final String streamKey;
    private final Optional<MeterRegistry> meterRegistry;

    public OutboxPublisher(
            OutboxRepository outboxRepository,
            StringRedisTemplate redisTemplate,
            @Value("${sandesh.messaging.stream-key}") String streamKey,
            @Autowired(required = false) MeterRegistry meterRegistry
    ) {
        this.outboxRepository = outboxRepository;
        this.redisTemplate = redisTemplate;
        this.streamKey = streamKey;
        this.meterRegistry = Optional.ofNullable(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${sandesh.messaging.outbox-poll-delay-ms:100}")
    @Transactional
    public void publishPendingEvents() {
        var events = outboxRepository.findReadyForPublish(Instant.now(), 100);

        if (events.isEmpty()) {
            return;
        }

        for (var event : events) {
            try {
                redisTemplate.opsForStream().add(
                        streamKey,
                        Map.of(
                                "eventId", event.getId().toString(),
                                "eventType", event.getEventType(),
                                "payloadJson", event.getPayloadJson()
                        )
                );
                event.markPublished();
                meterRegistry.ifPresent(m -> m.counter("sandesh.outbox.published").increment());
                logger.debug("Successfully published outbox event: id={}, type={}", event.getId(), event.getEventType());
            } catch (RuntimeException publishFailure) {
                event.scheduleRetry(publishFailure.getMessage());
                if (event.getStatus() == OutboxStatus.FAILED) {
                    meterRegistry.ifPresent(m -> m.counter("sandesh.outbox.failed").increment());
                    logger.error("Outbox event {} permanently failed after {} attempts: {}",
                            event.getId(), event.getAttempts(), publishFailure.getMessage());
                } else {
                    meterRegistry.ifPresent(m -> m.counter("sandesh.outbox.retries").increment());
                    logger.warn("Transient failure publishing outbox event {} (attempt {}): {}",
                            event.getId(), event.getAttempts(), publishFailure.getMessage());
                }
            }
        }
    }
}
