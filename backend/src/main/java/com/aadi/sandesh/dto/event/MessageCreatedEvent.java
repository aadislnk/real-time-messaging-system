package com.aadi.sandesh.dto.event;


import com.aadi.sandesh.dto.message.MessageResponse;

import java.util.List;
import java.util.UUID;

public record MessageCreatedEvent(
        MessageResponse message,
        List<UUID> recipientIds
) {
}
