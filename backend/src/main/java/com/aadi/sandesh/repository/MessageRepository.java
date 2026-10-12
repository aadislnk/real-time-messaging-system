package com.aadi.sandesh.repository;


import com.aadi.sandesh.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Optional<Message> findBySenderIdAndClientMessageId(UUID senderId, String clientMessageId);

    Optional<Message> findByIdAndConversationId(UUID id, UUID conversationId);
}