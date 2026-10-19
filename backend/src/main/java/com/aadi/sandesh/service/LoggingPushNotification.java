package com.aadi.sandesh.service;

import com.aadi.sandesh.domain.DeviceRegistration;
import com.aadi.sandesh.interfaces.PushNotificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LoggingPushNotification implements PushNotificationProvider {
    private final Logger logger = LoggerFactory.getLogger(LoggingPushNotification.class);

    @Override
    public void send(DeviceRegistration device, UUID conversationId, UUID messageId, String title, String body) {
        logger.info(
                "Push notification queued platform={} deviceId={} conversationId={} messageId={}",
                device.getPlatform(),
                device.getDeviceId(),
                conversationId,
                messageId
        );
    }
}