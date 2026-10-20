package com.aadi.sandesh.service;

import com.aadi.sandesh.domain.ConversationMember;
import com.aadi.sandesh.domain.UserAccount;
import com.aadi.sandesh.domain.enums.ConversationType;
import com.aadi.sandesh.domain.enums.EventType;
import com.aadi.sandesh.dto.event.RealtimeEvent;
import com.aadi.sandesh.messaging.RedisRealtimePublisher;
import com.aadi.sandesh.repository.ConversationMemberRepository;
import com.aadi.sandesh.repository.ConversationRepository;
import com.aadi.sandesh.repository.UserAccountRepository;
import com.aadi.sandesh.repository.UserBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TypingService {

    private final MembershipService membershipService;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserBlockRepository userBlockRepository;
    private final RedisRealtimePublisher publisher;

    @Transactional(readOnly = true)
    public void update(
            UUID actorId,
            UUID conversationId,
            boolean typing
    ) {
        membershipService.requireMember(conversationId, actorId);

        var conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null) {
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

        var blockedByActor = new HashSet<>(userBlockRepository.findBlockedRecipientIds(actorId, memberUserIds));
        var blockingActor = new HashSet<>(userBlockRepository.findRecipientsWhoBlockedSender(actorId, memberUserIds));

        if (conversation.getType() == ConversationType.DIRECT && (!blockedByActor.isEmpty() || !blockingActor.isEmpty())) {
            // Blocked users cannot exchange typing indicators in direct conversations
            return;
        }

        var eligibleRecipientIds = memberUserIds.stream()
                .filter(id -> !blockedByActor.contains(id) && !blockingActor.contains(id))
                .toList();

        if (eligibleRecipientIds.isEmpty()) {
            return;
        }

        var actorDisplayName = userAccountRepository.findById(actorId)
                .map(UserAccount::getDisplayName)
                .orElse("Someone");

        var eventType = typing ? EventType.TYPING_STARTED : EventType.TYPING_STOPPED;

        for (var targetUserId : eligibleRecipientIds) {
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
