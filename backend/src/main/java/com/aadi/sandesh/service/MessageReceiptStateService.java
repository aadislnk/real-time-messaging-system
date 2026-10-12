package com.aadi.sandesh.service;


import com.aadi.sandesh.domain.Message;
import com.aadi.sandesh.domain.MessageReceipt;
import com.aadi.sandesh.domain.enums.ReceiptState;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Service
public class MessageReceiptStateService {

    public Optional<ReceiptState> resolveForSender(
            Message message,
            UUID currentUserId,
            Collection<MessageReceipt> receipts
    ) {
        if (!message.getSenderId().equals(currentUserId)) {
            return Optional.empty();
        }

        if (receipts == null || receipts.isEmpty()) {
            return Optional.of(ReceiptState.SENT);
        }

        boolean allDelivered = receipts.stream()
                .allMatch(receipt -> receipt.getDeliveredAt() != null);

        if (!allDelivered) {
            return Optional.of(ReceiptState.SENT);
        }

        boolean allRead = receipts.stream()
                .allMatch(receipt -> receipt.getReadAt() != null);

        if (!allRead) {
            return Optional.of(ReceiptState.DELIVERED);
        }

        return Optional.of(ReceiptState.READ);
    }
}
