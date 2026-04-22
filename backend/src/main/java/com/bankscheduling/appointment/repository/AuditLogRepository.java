package com.bankscheduling.appointment.repository;

import com.bankscheduling.appointment.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("""
            select a from AuditLog a
            left join fetch a.performedBy
            order by a.createdAt desc
            """)
    List<AuditLog> findAllWithActorOrderByCreatedAtDesc();
}
