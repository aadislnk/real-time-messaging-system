package com.aadi.sandesh.service;

import com.aadi.sandesh.domain.ConversationMember;
import com.aadi.sandesh.domain.Message;
import com.aadi.sandesh.domain.MessageReceipt;
import com.aadi.sandesh.dto.message.MessagePageResponse;
import com.aadi.sandesh.dto.message.MessageResponse;
import com.aadi.sandesh.repository.ConversationMemberRepository;
import com.aadi.sandesh.repository.MessageReceiptRepository;
import com.aadi.sandesh.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageQueryService {

    private final MembershipService membershipService;
    private final MessageRepository messageRepository;
    private final MessageReceiptRepository messageReceiptRepository;
    private final MessageReceiptStateService messageReceiptStateService;
    private final ConversationMemberRepository conversationMemberRepository;

    @Transactional(readOnly = true)
    public MessagePageResponse getMessages(
            UUID userId,
            UUID conversationId,
            long afterSequence,
            int size
    ) {
        var member = membershipService.requireMember(
                conversationId,
                userId
        );

        int pageSize = Math.min(
                Math.max(size, 1),
                100
        );

        var currentMemberIds = conversationMemberRepository
                .findAllByConversationId(conversationId)
                .stream()
                .map(ConversationMember::getUserId)
                .toList();

        var pageable = PageRequest.of(0, pageSize);

        if (afterSequence > 0) {
            var slice = messageRepository.findMessagesAfterSequence(
                    conversationId,
                    afterSequence,
                    pageable
            );

            var messages = toResponses(
                    userId,
                    slice.getContent(),
                    currentMemberIds
            );

            Long nextAfterSequence = messages.isEmpty()
                    ? null
                    : messages.get(messages.size() - 1).sequenceNumber();

            return new MessagePageResponse(
                    messages,
                    nextAfterSequence,
                    slice.hasNext()
            );
        }

        var slice = messageRepository.findLatestMessages(
                conversationId,
                pageable
        );

        var messages = new ArrayList<>(toResponses(
                userId,
                slice.getContent(), currentMemberIds));

        Collections.reverse(messages);

        Long nextAfterSequence = messages.isEmpty()
                ? null
                : messages.get(messages.size() - 1).sequenceNumber();

        return new MessagePageResponse(
                messages,
                nextAfterSequence,
                slice.hasNext()
        );

    }

    private List<MessageResponse> toResponses(
            UUID userId,
            List<Message> messages,
            Collection<UUID> currentMemberIds
    ) {
        if (messages.isEmpty()) {
            return List.of();
        }

        var messageIds = messages.stream()
                .map(Message::getId)
                .toList();

        var receiptsByMessageId =
                messageReceiptRepository.findAllByMessageIdIn(messageIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                MessageReceipt::getMessageId
                        ));

        return messages.stream()
                .map(message -> MessageService.toResponse(
                        message,
                        messageReceiptStateService.resolveForSender(
                                message,
                                userId,
                                receiptsByMessageId.getOrDefault(
                                        message.getId(),
                                        List.of()
                                ),
                                currentMemberIds
                        ).orElse(null)
                )).toList();
    }
}