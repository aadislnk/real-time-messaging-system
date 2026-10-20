package com.aadi.sandesh.service;

import com.aadi.sandesh.dto.presence.PresenceResponse;
import com.aadi.sandesh.repository.ConversationMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PresenceService {

    private static final Logger logger = LoggerFactory.getLogger(PresenceService.class);

    private final StringRedisTemplate redisTemplate;
    private final ConversationMemberRepository memberRepository;
    private final Duration ttl;

    public PresenceService(
            StringRedisTemplate redisTemplate,
            ConversationMemberRepository memberRepository,
            @Value("${sandesh.presence.ttl:30s}") Duration ttl
    ) {
        this.redisTemplate = redisTemplate;
        this.memberRepository = memberRepository;
        this.ttl = ttl;
    }

    public void connected(UUID userId, String sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }

        try {
            var key = sessionKey(userId);
            long expiryTime = Instant.now().toEpochMilli() + ttl.toMillis();

            // Add or refresh session in Sorted Set with expiry score
            redisTemplate.opsForZSet().add(key, sessionId, (double) expiryTime);

            // Clean expired sessions
            cleanStaleSessions(key);

            // Set TTL on key
            redisTemplate.expire(key, ttl.multipliedBy(2));
        } catch (DataAccessException ex) {
            logger.warn("Redis outage while registering session connection for user {}: {}", userId, ex.getMessage());
        }
    }

    public boolean isOnline(UUID userId) {
        if (userId == null) {
            return false;
        }

        try {
            var key = sessionKey(userId);
            long now = Instant.now().toEpochMilli();

            // Purge expired sessions before checking
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, (double) now);

            Long activeCount = redisTemplate.opsForZSet().count(key, (double) (now + 1), Double.POSITIVE_INFINITY);
            return activeCount != null && activeCount > 0;
        } catch (DataAccessException ex) {
            logger.warn("Redis outage while querying presence status for user {}: {}", userId, ex.getMessage());
            return false;
        }
    }

    public PresenceResponse get(UUID userId) {
        try {
            boolean online = isOnline(userId);
            var rawLastSeen = redisTemplate.opsForValue().get(lastSeenKey(userId));
            Instant lastSeen = rawLastSeen == null ? null : Instant.parse(rawLastSeen);

            return new PresenceResponse(userId, online, lastSeen);
        } catch (DataAccessException ex) {
            logger.warn("Redis outage while fetching presence details for user {}: {}", userId, ex.getMessage());
            return new PresenceResponse(userId, false, null);
        }
    }

    public List<PresenceResponse> list(UUID conversationId) {
        return memberRepository
                .findAllByConversationId(conversationId)
                .stream()
                .map(member -> get(member.getUserId()))
                .toList();
    }

    public void heartbeat(UUID userId, String sessionId) {
        connected(userId, sessionId);
    }

    public void disconnected(UUID userId, String sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }

        try {
            var key = sessionKey(userId);
            long now = Instant.now().toEpochMilli();

            // Remove specific session
            redisTemplate.opsForZSet().remove(key, sessionId);

            // Clean any stale sessions
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, (double) now);

            Long remaining = redisTemplate.opsForZSet().count(key, (double) (now + 1), Double.POSITIVE_INFINITY);

            if (remaining == null || remaining == 0) {
                redisTemplate.delete(key);
                redisTemplate.opsForValue().set(lastSeenKey(userId), Instant.now().toString());
            } else {
                // Other active sessions still exist; refresh TTL on key
                redisTemplate.expire(key, ttl.multipliedBy(2));
            }
        } catch (DataAccessException ex) {
            logger.warn("Redis outage while disconnecting session for user {}: {}", userId, ex.getMessage());
        }
    }

    private void cleanStaleSessions(String key) {
        long now = Instant.now().toEpochMilli();
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, (double) now);
    }

    private String sessionKey(UUID userId) {
        return "presence:user:" + userId + ":sessions";
    }

    private String lastSeenKey(UUID userId) {
        return "presence:user:" + userId + ":last-seen";
    }
}
