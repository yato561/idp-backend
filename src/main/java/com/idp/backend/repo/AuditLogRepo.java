package com.idp.backend.repo;

import com.idp.backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepo extends JpaRepository<AuditLog, Long> {
}
