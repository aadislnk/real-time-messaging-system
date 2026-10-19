package com.aadi.sandesh.service;

import com.aadi.sandesh.dto.message.MessageResponse;
import com.aadi.sandesh.interfaces.PushNotificationProvider;
import com.aadi.sandesh.repository.ConversationMemberRepository;
import com.aadi.sandesh.repository.DeviceRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final DeviceRegistrationRepository deviceRepository;
    private final ConversationMemberRepository memberRepository;
    private final PushNotificationProvider pushProvider;

    @Transactional(readOnly = true)
    public void notifyOfflineUser(
            UUID userId,
            MessageResponse message
    ) {
        var member = memberRepository.findByConversationIdAndUserId(
                message.conversationId(),
                userId
        );

        if (member.isEmpty()) {
            return;
        }

        if (member.get().isMutedAt(Instant.now())) {
            return;
        }

        deviceRepository.findAllByUserIdAndNotificationsEnabledTrue(userId)
                .stream()
                .filter(
                        device -> device.getPushToken() != null
                                && !device.getPushToken().isBlank()
                )
                .forEach(device ->
                        pushProvider.send(
                                device,
                                message.conversationId(),
                                message.id(),
                                "New SANDESH message",
                                "Open SANDESH to view your message"
                        ));
    }
}