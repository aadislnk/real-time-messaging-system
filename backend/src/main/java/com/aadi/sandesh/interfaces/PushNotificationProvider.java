package com.aadi.sandesh.interfaces;

import com.aadi.sandesh.domain.DeviceRegistration;

import java.util.UUID;

public interface PushNotificationProvider {
    void send(
            DeviceRegistration device,
            UUID conversationId,
            UUID messageId,
            String title,
            String body
    );
}
