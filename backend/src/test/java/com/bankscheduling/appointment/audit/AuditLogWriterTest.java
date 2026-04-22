package com.bankscheduling.appointment.audit;

import com.bankscheduling.appointment.entity.AuditAction;
import com.bankscheduling.appointment.entity.AuditLog;
import com.bankscheduling.appointment.entity.Branch;
import com.bankscheduling.appointment.repository.AuditLogRepository;
import com.bankscheduling.appointment.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogWriterTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private AuditLogWriter auditLogWriter;

    @Test
    void appendPersistsAuditRow() {
        Branch branch = new Branch();
        branch.setCode("HQ");
        branch.setDisplayName("Headquarters");
        branch.setId(42L);

        auditLogWriter.append(AuditAction.INSERT, branch, null, auditLogWriter.snapshot(branch));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("INSERT");
        assertThat(captor.getValue().getEntityType()).isEqualTo("Branch");
        assertThat(captor.getValue().getEntityId()).isEqualTo("42");
    }
}
