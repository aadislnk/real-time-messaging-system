package com.aadi.sandesh.repository;

import com.aadi.sandesh.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

}
