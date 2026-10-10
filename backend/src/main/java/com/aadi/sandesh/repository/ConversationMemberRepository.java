package com.aadi.sandesh.repository;

import com.aadi.sandesh.domain.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, UUID> {
    Optional<ConversationMember> findByConversationIdAndUserId(UUID conversationId, UUID userId);
}
