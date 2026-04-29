package com.bankscheduling.appointment.service;

import com.bankscheduling.appointment.dto.appointment.AppointmentResponseDto;
import com.bankscheduling.appointment.entity.Appointment;
import com.bankscheduling.appointment.entity.ServiceType;
import com.bankscheduling.appointment.entity.User;
import com.bankscheduling.appointment.repository.AppointmentRepository;
import com.bankscheduling.appointment.repository.UserRepository;
import com.bankscheduling.appointment.security.SchedulingAuthorities;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    public AppointmentService(AppointmentRepository appointmentRepository, UserRepository userRepository) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns appointments for a branch after verifying the caller belongs to that branch,
     * unless they hold a global admin role ({@link SchedulingAuthorities}).
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> getAppointmentsForBranch(Long branchId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authentication required");
        }

        Long userId;
        try {
            userId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new AccessDeniedException("Invalid security principal");
        }

        User current = userRepository.findByIdWithBranchAndRole(userId)
                .orElseThrow(() -> new AccessDeniedException("Employee not found"));

        String roleName = current.getRole().getName();
        boolean bypassBranchScope = SchedulingAuthorities.mayAccessAllBranches(roleName);
        if (!bypassBranchScope && !current.getBranch().getId().equals(branchId)) {
            throw new AccessDeniedException("Not permitted to view appointments for this branch");
        }

        return appointmentRepository.findAllByBranchIdWithAssociationsOrdered(branchId).stream()
                .map(this::toDto)
                .toList();
    }

    private AppointmentResponseDto toDto(Appointment a) {
        User c = a.getCustomer();
        User e = a.getEmployee();
        ServiceType st = a.getServiceType();
        return new AppointmentResponseDto(
                a.getId(),
                a.getBranch().getId(),
                new AppointmentResponseDto.CustomerSummary(
                        c.getId(),
                        c.getFullName(),
                        c.getEmailNormalized(),
                        c.getPhone()
                ),
                new AppointmentResponseDto.EmployeeSummary(e.getId(), emptyIfNull(e.getFirstName()), emptyIfNull(e.getLastName())),
                new AppointmentResponseDto.ServiceTypeSummary(st.getId(), st.getCode(), st.getDisplayName()),
                a.getScheduledStart(),
                a.getScheduledEnd(),
                a.getStatus().name(),
                a.getNotes(),
                a.getOptimisticLockVersion()
        );
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
