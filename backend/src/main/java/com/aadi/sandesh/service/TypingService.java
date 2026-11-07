package com.aadi.sandesh.service;

import com.aadi.sandesh.domain.ConversationMember;
import com.aadi.sandesh.domain.UserAccount;
import com.aadi.sandesh.domain.enums.EventType;
import com.aadi.sandesh.dto.event.RealtimeEvent;
import com.aadi.sandesh.messaging.RedisRealtimePublisher;
import com.aadi.sandesh.repository.ConversationMemberRepository;
import com.aadi.sandesh.repository.ConversationRepository;
import com.aadi.sandesh.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TypingService {

    private final MembershipService membershipService;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserAccountRepository userAccountRepository;
    private final RedisRealtimePublisher publisher;

    @Transactional(readOnly = true)
    public void update(
            UUID actorId,
            UUID conversationId,
            boolean typing
    ) {
        membershipService.requireMember(conversationId, actorId);

        if (conversationRepository.findById(conversationId).isEmpty()) {
            return;
        }

        var memberUserIds = memberRepository.findAllByConversationId(conversationId)
                .stream()
                .map(ConversationMember::getUserId)
                .filter(userId -> !userId.equals(actorId))
                .toList();

        if (memberUserIds.isEmpty()) {
            return;
        }

        var actorDisplayName = userAccountRepository.findById(actorId)
                .map(UserAccount::getDisplayName)
                .orElse("Someone");

        var eventType = typing ? EventType.TYPING_STARTED : EventType.TYPING_STOPPED;

        for (var targetUserId : memberUserIds) {
            publisher.publish(
                    new RealtimeEvent(
                            eventType,
                            targetUserId,
                            conversationId,
                            actorId,
                            null,
                            Map.of(
                                    "actorDisplayName", actorDisplayName,
                                    "expiresInSeconds", 5
                            ),
                            Instant.now()
                    )
            );
        }
    }
}
