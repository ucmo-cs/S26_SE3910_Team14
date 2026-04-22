package com.bankscheduling.appointment.audit;

import com.bankscheduling.appointment.entity.AuditAction;
import com.bankscheduling.appointment.entity.AuditLog;
import com.bankscheduling.appointment.entity.AuditableEntity;
import com.bankscheduling.appointment.entity.CustomerAccount;
import com.bankscheduling.appointment.entity.Employee;
import com.bankscheduling.appointment.repository.AuditLogRepository;
import com.bankscheduling.appointment.repository.CustomerAccountRepository;
import com.bankscheduling.appointment.repository.EmployeeRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuditLogWriter {

    private final AuditLogRepository auditLogRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerAccountRepository customerAccountRepository;

    public AuditLogWriter(
            AuditLogRepository auditLogRepository,
            EmployeeRepository employeeRepository,
            CustomerAccountRepository customerAccountRepository
    ) {
        this.auditLogRepository = auditLogRepository;
        this.employeeRepository = employeeRepository;
        this.customerAccountRepository = customerAccountRepository;
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
        populateActorMetadata(row);
        populateRequestMetadata(row);
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

    private void populateActorMetadata(AuditLog row) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            row.setActorType("SYSTEM");
            return;
        }

        Long employeeId = parsePrincipalAsEmployeeId(authentication.getPrincipal());
        if (employeeId == null) {
            row.setActorType("SYSTEM");
            return;
        }

        Optional<Employee> actor = employeeRepository.findById(employeeId);
        if (actor.isEmpty()) {
            populateCustomerActorMetadata(row, employeeId, authentication);
            return;
        }

        row.setActorType("EMPLOYEE");
        row.setActorEmployeeId(employeeId);
        row.setPerformedBy(actor.orElse(null));
        actor.ifPresent(employee -> hydrateActor(row, employee, authentication));
        if (row.getActorRole() == null) {
            row.setActorRole(authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(",")));
        }
    }

    private void populateCustomerActorMetadata(AuditLog row, Long principalId, Authentication authentication) {
        Optional<CustomerAccount> customerActor = customerAccountRepository.findByIdWithCustomer(principalId);
        if (customerActor.isPresent()) {
            CustomerAccount account = customerActor.get();
            row.setActorType("CUSTOMER");
            row.setActorUsername(account.getEmailNormalized());
            row.setActorEmail(account.getEmailNormalized());
            if (account.getRole() != null && account.getRole().getName() != null) {
                row.setActorRole(account.getRole().getName());
                return;
            }
        }

        row.setActorType("AUTHENTICATED");
        row.setActorUsername(String.valueOf(authentication.getPrincipal()));
        row.setActorRole(authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(",")));
    }

    private void hydrateActor(AuditLog row, Employee employee, Authentication authentication) {
        row.setActorUsername(employee.getUsername());
        row.setActorEmail(employee.getWorkEmail());
        if (employee.getRole() != null && employee.getRole().getName() != null) {
            row.setActorRole(employee.getRole().getName());
            return;
        }
        row.setActorRole(authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(",")));
    }

    private void populateRequestMetadata(AuditLog row) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletAttributes)) {
            return;
        }

        HttpServletRequest request = servletAttributes.getRequest();
        row.setRequestMethod(request.getMethod());
        row.setRequestPath(request.getRequestURI());
        row.setIpAddress(extractIpAddress(request));
        row.setCorrelationId(extractCorrelationId(request).orElse(null));
        row.setUserAgent(request.getHeader("User-Agent"));
    }

    private static Long parsePrincipalAsEmployeeId(Object principal) {
        if (principal == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(principal));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String extractIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private static Optional<String> extractCorrelationId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        if (requestId != null && !requestId.isBlank()) {
            return Optional.of(requestId.trim());
        }
        String traceId = request.getHeader("X-Correlation-ID");
        if (traceId != null && !traceId.isBlank()) {
            return Optional.of(traceId.trim());
        }
        return Optional.empty();
    }
}
