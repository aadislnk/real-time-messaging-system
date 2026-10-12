package com.aadi.sandesh.service;

import com.aadi.sandesh.api.dto.MessageResponse;
import com.aadi.sandesh.api.dto.SendMessageRequest;
import com.aadi.sandesh.domain.Message;
import com.aadi.sandesh.domain.MessageReceipt;
import com.aadi.sandesh.domain.enums.MessageType;
import com.aadi.sandesh.domain.enums.ReceiptState;
import com.aadi.sandesh.exception.ConflictException;
import com.aadi.sandesh.exception.ForbiddenException;
import com.aadi.sandesh.exception.NotFoundException;
import com.aadi.sandesh.repository.ConversationMemberRepository;
import com.aadi.sandesh.repository.ConversationRepository;
import com.aadi.sandesh.repository.MessageReceiptRepository;
import com.aadi.sandesh.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MembershipService membershipService;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository cmRepository;
    private final MessageReceiptRepository mrRepository;
    private final MessageReceiptStateService receiptStateService;

    @Transactional
    public MessageResponse send(
            UUID senderId,
            UUID conversationId,
            SendMessageRequest request
    ) {
        membershipService.requireMember(conversationId, senderId);

        var existing = messageRepository
                .findBySenderIdAndClientMessageId(senderId, request.clientMessageId());

        if (existing.isPresent()) {
            var message = existing.get();

            // The same clientMessageId must not be reused
            // for different content or a different conversation

            if (
                    !message.getConversationId().equals(conversationId)
                            || !message.getContent().equals(request.content().strip())
            ) {
                throw new ConflictException(
                        "clientMessageId was already used for different content"
                );
            }

            ReceiptState existingState =
                    receiptStateService.resolveForSender(
                            message,
                            senderId,
                            mrRepository.findAllByMessageId(message.getId())
                    ).orElseThrow(
                            () -> new ForbiddenException("You are not allowed to view the state of message"));

            return toResponse(message, existingState);
        }

        var conversation = conversationRepository.findByIdForUpdate(conversationId)
                .orElseThrow(() ->
                        new NotFoundException("Conversation not found"));

        long sequenceNumber = conversation.allocateNextMessageSequence();

        var message = new Message(
                conversationId,
                senderId,
                request.clientMessageId(),
                sequenceNumber,
                MessageType.TEXT,
                request.content().strip()
        );

        messageRepository.save(message);

        var recipientIds = cmRepository.findRecipientIds(conversationId, senderId);

        if (!recipientIds.isEmpty()) {
            mrRepository.saveAll(
                    recipientIds.stream()
                            .map(recipientId ->
                                    new MessageReceipt(
                                            message.getId(),
                                            recipientId
                                    ))
                            .toList()
            );
        }

        return toResponse(message, ReceiptState.SENT);
    }

    public static MessageResponse toResponse(Message message, ReceiptState state) {
        return new MessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getSenderId(),
                message.getClientMessageId(),
                message.getSequenceNumber(),
                message.getMessageType(),
                message.getContent(),
                message.getCreatedAt(),
                state
        );
    }
}