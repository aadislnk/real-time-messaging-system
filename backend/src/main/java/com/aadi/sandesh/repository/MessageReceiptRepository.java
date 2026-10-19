package com.aadi.sandesh.repository;

import com.aadi.sandesh.domain.MessageReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageReceiptRepository extends JpaRepository<MessageReceipt, UUID> {
    List<MessageReceipt> findAllByMessageId(UUID messageId);

    Optional<MessageReceipt> findByMessageIdAndUserId(UUID messageId, UUID userId);

    List<MessageReceipt> findAllByMessageIdIn(Collection<UUID> messageIds);
}