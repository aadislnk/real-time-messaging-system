package com.aadi.sandesh.service;

import com.aadi.sandesh.domain.enums.EventType;
import com.aadi.sandesh.dto.event.MessageCreatedEvent;
import com.aadi.sandesh.dto.event.RealtimeEvent;
import com.aadi.sandesh.messaging.RedisRealtimePublisher;
import com.aadi.sandesh.repository.ProcessedEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class MessageDeliveryWorker {

    private static final Logger logger = LoggerFactory.getLogger(MessageDeliveryWorker.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final RedisRealtimePublisher redisRealtimePublisher;
    private final PresenceService presenceService;
    private final NotificationService notificationService;
    private final Optional<MeterRegistry> meterRegistry;

    private final String streamKey;
    private final String consumerGroup;
    private final String consumerName;
    private final AtomicBoolean groupInitialized = new AtomicBoolean(false);

    public MessageDeliveryWorker(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            ProcessedEventRepository processedEventRepository,
            RedisRealtimePublisher redisRealtimePublisher,
            PresenceService presenceService,
            NotificationService notificationService,
            @Value("${sandesh.messaging.stream-key}") String streamKey,
            @Value("${sandesh.messaging.consumer-group}") String consumerGroup,
            @Value("${sandesh.messaging.consumer-name}") String consumerName,
            @Autowired(required = false) MeterRegistry meterRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.processedEventRepository = processedEventRepository;
        this.redisRealtimePublisher = redisRealtimePublisher;
        this.presenceService = presenceService;
        this.notificationService = notificationService;
        this.streamKey = streamKey;
        this.consumerGroup = consumerGroup;
        this.consumerName = consumerName;
        this.meterRegistry = Optional.ofNullable(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${sandesh.messaging.delivery-poll-delay-ms:100}")
    public void consume() {
        if (!ensureConsumerGroup()) {
            return;
        }

        List<MapRecord<String, Object, Object>> records;
        try {
            records = streams().read(
                    Consumer.from(consumerGroup, consumerName),
                    StreamReadOptions.empty().count(100),
                    StreamOffset.create(streamKey, ReadOffset.lastConsumed())
            );
        } catch (RuntimeException redisUnavailable) {
            logger.warn("Redis stream read failed: {}", redisUnavailable.getMessage());
            return;
        }

        if (records == null || records.isEmpty()) {
            return;
        }

        for (var record : records) {
            process(record);
        }
    }

    @Scheduled(fixedDelay = 5_000)
    public void recoverPending() {
        if (!ensureConsumerGroup()) {
            return;
        }

        try {
            // 1. Process consumer's own pending messages (delivered but unacknowledged)
            var ownPending = streams().read(
                    Consumer.from(consumerGroup, consumerName),
                    StreamReadOptions.empty().count(100),
                    StreamOffset.create(streamKey, ReadOffset.from("0"))
            );

            if (ownPending != null && !ownPending.isEmpty()) {
                ownPending.forEach(this::process);
            }

            // 2. Claim messages idle for > 30 seconds from other crashed consumers
            var pendingSummary = streams().pending(streamKey, consumerGroup, Range.unbounded(), 100);
            if (pendingSummary != null) {
                var staleIds = pendingSummary.stream()
                        .filter(this::isStaleFromAnotherConsumer)
                        .map(PendingMessage::getId)
                        .toArray(RecordId[]::new);

                if (staleIds.length > 0) {
                    var claimed = streams().claim(
                            streamKey,
                            consumerGroup,
                            consumerName,
                            Duration.ofSeconds(30),
                            staleIds
                    );
                    if (claimed != null) {
                        claimed.forEach(this::process);
                    }
                }
            }
        } catch (RuntimeException ex) {
            logger.warn("Error during pending message recovery: {}", ex.getMessage());
        }
    }

    private boolean isStaleFromAnotherConsumer(PendingMessage pendingMessage) {
        return !consumerName.equals(pendingMessage.getConsumerName())
                && pendingMessage.getElapsedTimeSinceLastDelivery().compareTo(Duration.ofSeconds(30)) >= 0;
    }

    private void process(MapRecord<String, ?, ?> record) {
        var valueMap = record.getValue();
        var eventIdRaw = String.valueOf(valueMap.get("eventId"));
        var eventType = String.valueOf(valueMap.get("eventType"));
        var payloadJson = String.valueOf(valueMap.get("payloadJson"));

        UUID eventId;
        EventType type;

        // Validation for poison pill events
        try {
            eventId = UUID.fromString(eventIdRaw);
            type = EventType.valueOf(eventType);
        } catch (Exception ex) {
            logger.error("Poison pill stream record {}: invalid eventId or eventType. Dead-lettering: {}", record.getId(), ex.getMessage());
            meterRegistry.ifPresent(m -> m.counter("sandesh.delivery.dead_letters").increment());
            acknowledge(record);
            return;
        }

        // Idempotency check: if already processed, simply acknowledge and skip
        if (processedEventRepository.existsById(eventId)) {
            logger.debug("Event {} was already processed; acknowledging duplicate", eventId);
            acknowledge(record);
            return;
        }

        try {
            switch (type) {
                case MESSAGE_CREATED -> {
                    var event = objectMapper.readValue(payloadJson, MessageCreatedEvent.class);

                    for (var recipientId : event.recipientIds()) {
                        var realtimeEvent = new RealtimeEvent(
                                EventType.MESSAGE_CREATED,
                                recipientId,
                                event.message().conversationId(),
                                event.message().senderId(),
                                event.message().id(),
                                event.message(),
                                Instant.now()
                        );

                        // Realtime event must always be published regardless of presence
                        redisRealtimePublisher.publish(realtimeEvent);

                        // Presence only determines whether offline notification is also sent
                        if (!presenceService.isOnline(recipientId)) {
                            notificationService.notifyOfflineUser(recipientId, event.message());
                        }
                    }
                }

                case CONVERSATION_CREATED,
                     CONVERSATION_UPDATED,
                     CONVERSATION_REMOVED,
                     MESSAGE_DELIVERED,
                     MESSAGE_READ -> {
                    var event = objectMapper.readValue(payloadJson, RealtimeEvent.class);
                    redisRealtimePublisher.publish(event);
                }

                default -> {
                    logger.warn("Unrecognized stream event type: {}; acknowledging and dead-lettering", eventType);
                    meterRegistry.ifPresent(m -> m.counter("sandesh.delivery.dead_letters").increment());
                    saveProcessedAndAcknowledge(eventId, record);
                    return;
                }
            }

            saveProcessedAndAcknowledge(eventId, record);
            meterRegistry.ifPresent(m -> m.counter("sandesh.delivery.processed").increment());
        } catch (tools.jackson.core.JacksonException jsonEx) {
            logger.error("Malformed payload for event {}: dead-lettering to avoid blocking stream: {}", eventId, jsonEx.getMessage());
            meterRegistry.ifPresent(m -> m.counter("sandesh.delivery.dead_letters").increment());
            saveProcessedAndAcknowledge(eventId, record);
        } catch (Exception ex) {
            logger.error("Failed to process delivery record {} for event {}: {}", record.getId(), eventId, ex.getMessage(), ex);
            meterRegistry.ifPresent(m -> m.counter("sandesh.delivery.errors").increment());
            // Do NOT acknowledge on transient failure: message will remain pending and be retried
        }
    }

    @Transactional
    protected void saveProcessedAndAcknowledge(UUID eventId, MapRecord<String, ?, ?> record) {
        processedEventRepository.markProcessed(eventId, Instant.now());
        acknowledge(record);
    }

    private void acknowledge(MapRecord<String, ?, ?> record) {
        try {
            streams().acknowledge(streamKey, consumerGroup, record.getId());
        } catch (Exception ex) {
            logger.warn("Failed to acknowledge stream record {}: {}", record.getId(), ex.getMessage());
        }
    }

    private boolean ensureConsumerGroup() {
        if (groupInitialized.get()) {
            return true;
        }

        try {
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(streamKey))) {
                // Stream not yet created by publisher
                return false;
            }

            try {
                var groups = streams().groups(streamKey);
                if (groups != null && groups.stream().anyMatch(g -> consumerGroup.equals(g.groupName()))) {
                    groupInitialized.set(true);
                    return true;
                }
            } catch (Exception ignored) {
                // Ignore if XINFO is not supported or fails
            }

            streams().createGroup(streamKey, ReadOffset.from("0-0"), consumerGroup);
            groupInitialized.set(true);
            return true;
        } catch (Exception ex) {
            Throwable root = ex;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            String fullError = (ex.getMessage() + " " + root.getMessage()).toUpperCase();
            if (fullError.contains("BUSYGROUP") || fullError.contains("ALREADY EXISTS")) {
                groupInitialized.set(true);
                return true;
            }
            logger.debug("Consumer group initialization pending: {}", fullError);
            return false;
        }
    }

    private StreamOperations<String, Object, Object> streams() {
        return redisTemplate.opsForStream();
    }
}
