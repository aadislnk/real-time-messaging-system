package com.aadi.sandesh.service;

import com.aadi.sandesh.domain.AuditEvent;
import com.aadi.sandesh.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public void record(UUID actorId, String action, String resourceType, String resourceId, Map<String, ?> metadata) {
        auditEventRepository.save(
                new AuditEvent(actorId, action, resourceType, resourceId, toJson(metadata)));

    }

    private String toJson(Map<String, ?> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JacksonException e) {
            return "{}";
        }
    }
}
