package com.aadi.sandesh.service;

import com.aadi.sandesh.domain.ConversationMember;
import com.aadi.sandesh.domain.UserAccount;
import com.aadi.sandesh.domain.enums.EventType;
import com.aadi.sandesh.dto.event.RealtimeEvent;
import com.aadi.sandesh.messaging.RedisRealtimePublisher;
import com.aadi.sandesh.repository.ConversationMemberRepository;
import com.aadi.sandesh.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TypingService {

    private final MembershipService membershipService;
    private final ConversationMemberRepository memberRepository;
    private final UserAccountRepository userAccountRepository;
    private final RedisRealtimePublisher publisher;

    public void update(
            UUID actorId,
            UUID conversationId,
            boolean typing
    ) {
        membershipService.requireMember(conversationId, actorId);

        var actorDisplayName = userAccountRepository.findById(actorId)
                .map(UserAccount::getDisplayName)
                .orElse("Someone");

        memberRepository.findAllByConversationId(conversationId)
                .stream()
                .map(ConversationMember::getUserId)
                .filter(userId -> !userId.equals(actorId))
                .forEach(targetUserId -> {
                    var evenType = typing
                            ? EventType.TYPING_STARTED
                            : EventType.TYPING_STOPPED;

                    publisher.publish(
                            new RealtimeEvent(
                                    evenType,
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
                });
    }
}
