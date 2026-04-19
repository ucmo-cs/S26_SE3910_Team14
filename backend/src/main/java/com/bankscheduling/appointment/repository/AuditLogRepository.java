package com.bankscheduling.appointment.repository;

import com.bankscheduling.appointment.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
