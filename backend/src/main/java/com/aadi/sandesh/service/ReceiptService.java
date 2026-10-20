package com.aadi.sandesh.service;

import com.aadi.sandesh.domain.ConversationMember;
import com.aadi.sandesh.domain.OutboxEvent;
import com.aadi.sandesh.domain.enums.EventType;
import com.aadi.sandesh.domain.enums.ReceiptType;
import com.aadi.sandesh.domain.enums.ResourceType;
import com.aadi.sandesh.dto.event.RealtimeEvent;
import com.aadi.sandesh.dto.message.ReceiptRequest;
import com.aadi.sandesh.dto.message.ReceiptResponse;
import com.aadi.sandesh.exception.ForbiddenException;
import com.aadi.sandesh.exception.NotFoundException;
import com.aadi.sandesh.repository.ConversationMemberRepository;
import com.aadi.sandesh.repository.MessageReceiptRepository;
import com.aadi.sandesh.repository.MessageRepository;
import com.aadi.sandesh.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final MembershipService membershipService;
    private final MessageRepository messageRepository;
    private final MessageReceiptRepository receiptRepository;
    private final ConversationMemberRepository memberRepository;
    private final MessageReceiptStateService mrStateService;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ReceiptResponse acknowledge(
            UUID userId,
            UUID conversationId,
            UUID messageId,
            ReceiptRequest request
    ) {
        membershipService.requireMember(conversationId, userId);

        var message = messageRepository
                .findByIdAndConversationId(messageId, conversationId)
                .orElseThrow(() -> new NotFoundException("Message not found"));

        var receipts = receiptRepository.findAllByMessageIdForUpdate(messageId);

        var receipt = receipts.stream()
                .filter(item -> item.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Receipt not found for this user"));

        var currentMemberIds = memberRepository
                .findAllByConversationId(conversationId)
                .stream()
                .map(ConversationMember::getUserId)
                .toList();

        boolean stateChanged = false;
        var now = Instant.now();

        switch (request.type()) {
            case DELIVERED -> {
                // Monotonic: if already delivered or read, transition is a no-op
                if (receipt.getDeliveredAt() == null && receipt.getReadAt() == null) {
                    receipt.markDelivered(now);
                    stateChanged = true;
                }
            }

            case READ -> {
                // Monotonic: if already read, transition is a no-op
                if (receipt.getReadAt() == null) {
                    receipt.markRead(now);
                    stateChanged = true;

                    var member = memberRepository
                            .findByConversationIdAndUserId(conversationId, userId)
                            .orElseThrow(() -> new NotFoundException("Conversation member not found"));

                    member.markReadThrough(message.getSequenceNumber());
                    memberRepository.save(member);
                }
            }
        }

        var receiptState = mrStateService.resolveForSender(
                message,
                message.getSenderId(),
                receipts,
                currentMemberIds
        ).orElseThrow(() -> new ForbiddenException("You are not allowed to view the state of message"));

        var response = new ReceiptResponse(
                receipt.getMessageId(),
                receipt.getUserId(),
                receipt.getDeliveredAt(),
                receipt.getReadAt(),
                receiptState
        );

        // Only emit realtime event and write outbox if state actually changed (idempotency)
        if (stateChanged) {
            var eventType = request.type() == ReceiptType.DELIVERED
                    ? EventType.MESSAGE_DELIVERED
                    : EventType.MESSAGE_READ;

            outboxRepository.save(
                    new OutboxEvent(
                            ResourceType.MESSAGE.name(),
                            message.getId(),
                            eventType.name(),
                            toJson(
                                    new RealtimeEvent(
                                            eventType,
                                            message.getSenderId(),
                                            conversationId,
                                            userId,
                                            message.getId(),
                                            response,
                                            Instant.now()
                                    )
                            )
                    )
            );
        }

        return response;
    }

    @Transactional
    public void recalculateForRemovedMember(
            UUID conversationId,
            UUID removedUserId
    ) {
        var messages = messageRepository.findMessageWithReceiptForUser(
                conversationId, removedUserId
        );

        if (messages.isEmpty()) {
            return;
        }

        var currentMemberIds = memberRepository
                .findAllByConversationId(conversationId)
                .stream()
                .map(ConversationMember::getUserId)
                .toList();

        for (var message : messages) {
            var receipts = receiptRepository.findAllByMessageIdForUpdate(message.getId());

            var receiptState = mrStateService.resolveForSender(
                    message,
                    message.getSenderId(),
                    receipts,
                    currentMemberIds
            ).orElse(null);

            if (receiptState == null) {
                continue;
            }

            var eventType = switch (receiptState) {
                case DELIVERED -> EventType.MESSAGE_DELIVERED;
                case READ -> EventType.MESSAGE_READ;
                case SENT -> null;
            };

            if (eventType == null) {
                continue;
            }

            var response = new ReceiptResponse(
                    message.getId(),
                    message.getSenderId(),
                    null,
                    null,
                    receiptState
            );

            outboxRepository.save(
                    new OutboxEvent(
                            ResourceType.MESSAGE.name(),
                            message.getId(),
                            eventType.name(),
                            toJson(
                                    new RealtimeEvent(
                                            eventType,
                                            message.getSenderId(),
                                            conversationId,
                                            removedUserId,
                                            message.getId(),
                                            response,
                                            Instant.now()
                                    )
                            )
                    )
            );
        }
    }

    private String toJson(RealtimeEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize receipt event", exception);
        }
    }
}
