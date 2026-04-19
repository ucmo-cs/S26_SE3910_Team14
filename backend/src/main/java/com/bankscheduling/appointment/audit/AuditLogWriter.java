package com.bankscheduling.appointment.audit;

import com.bankscheduling.appointment.entity.AuditAction;
import com.bankscheduling.appointment.entity.AuditLog;
import com.bankscheduling.appointment.entity.AuditableEntity;
import com.bankscheduling.appointment.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuditLogWriter {

    private final AuditLogRepository auditLogRepository;

    public AuditLogWriter(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Persists an audit row in a new transaction so failures in the primary unit of work
     * do not roll back the immutable audit record (tunable for your policy).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void append(AuditAction action, AuditableEntity entity, Map<String, Object> oldState, Map<String, Object> newState) {
        AuditLog row = new AuditLog();
        row.setEntityType(entity.getClass().getSimpleName());
        row.setEntityId(entity.getId() == null ? "unknown" : String.valueOf(entity.getId()));
        row.setAction(action.name());
        row.setOldState(oldState);
        row.setNewState(newState);
        auditLogRepository.save(row);
    }

    public Map<String, Object> snapshot(AuditableEntity entity) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entity.getId());
        map.put("type", entity.getClass().getSimpleName());
        map.put("createdAt", entity.getCreatedAt());
        map.put("updatedAt", entity.getUpdatedAt());
        return map;
    }
}
