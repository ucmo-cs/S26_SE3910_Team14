package com.bankscheduling.appointment.audit;

import com.bankscheduling.appointment.entity.AuditAction;
import com.bankscheduling.appointment.entity.AuditableEntity;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

/**
 * Works together with {@link org.springframework.data.jpa.domain.support.AuditingEntityListener}
 * on {@link AuditableEntity} to append rows to {@code audit_logs} for insert, update, and delete.
 * Instantiated by the JPA provider; resolves collaborators via {@link SpringContextHolder}.
 */
public class AuditPersistenceListener {

    @PostPersist
    public void afterInsert(AuditableEntity entity) {
        AuditLogWriter writer = SpringContextHolder.getBean(AuditLogWriter.class);
        writer.append(AuditAction.INSERT, entity, null, writer.snapshot(entity));
    }

    @PostUpdate
    public void afterUpdate(AuditableEntity entity) {
        AuditLogWriter writer = SpringContextHolder.getBean(AuditLogWriter.class);
        writer.append(AuditAction.UPDATE, entity, writer.snapshot(entity), writer.snapshot(entity));
    }

    @PostRemove
    public void afterDelete(AuditableEntity entity) {
        AuditLogWriter writer = SpringContextHolder.getBean(AuditLogWriter.class);
        writer.append(AuditAction.DELETE, entity, writer.snapshot(entity), null);
    }
}
