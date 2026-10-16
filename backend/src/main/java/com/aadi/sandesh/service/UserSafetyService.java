package com.aadi.sandesh.service;


import com.aadi.sandesh.domain.UserBlock;
import com.aadi.sandesh.exception.ConflictException;
import com.aadi.sandesh.exception.NotFoundException;
import com.aadi.sandesh.repository.UserAccountRepository;
import com.aadi.sandesh.repository.UserBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSafetyService {
    private final UserAccountRepository userAccountRepository;
    private final UserBlockRepository userBlockRepository;
    private final AuditService auditService;

    @Transactional
    public void block(UUID blockerId, UUID blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new ConflictException("A user cannot block themselves");
        }

        if (!userAccountRepository.existsById(blockedId)) {
            throw new NotFoundException("User not found");
        }

        if (!userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            userBlockRepository.save(new UserBlock(blockerId, blockedId));
        }

        auditService.record(
                blockerId,
                "USER_BLOCKED",
                "USER",
                blockedId.toString(),
                Map.of()
        );
    }

    @Transactional(readOnly = true)
    public boolean isBlocked(UUID blockerId, UUID blockedId) {
        return userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Transactional
    public void unblock(UUID blockerId, UUID blockedId) {
        userBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);

        auditService.record(
                blockerId,
                "USER_UNBLOCKED",
                "USER",
                blockedId.toString(),
                Map.of()
        );
    }
}
