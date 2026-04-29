package com.bankscheduling.appointment.service;

import com.bankscheduling.appointment.dto.customerauth.CustomerAppointmentDto;
import com.bankscheduling.appointment.dto.customerauth.CustomerAuthRequest;
import com.bankscheduling.appointment.dto.customerauth.CustomerAuthResponse;
import com.bankscheduling.appointment.dto.customerauth.CustomerProfileDto;
import com.bankscheduling.appointment.dto.customerauth.CustomerRegisterRequest;
import com.bankscheduling.appointment.dto.appointment.CustomerAppointmentUpdateRequest;
import com.bankscheduling.appointment.entity.Appointment;
import com.bankscheduling.appointment.entity.AppointmentStatus;
import com.bankscheduling.appointment.entity.BranchBusinessHours;
import com.bankscheduling.appointment.entity.Role;
import com.bankscheduling.appointment.entity.User;
import com.bankscheduling.appointment.repository.AppointmentRepository;
import com.bankscheduling.appointment.repository.BranchBusinessHoursRepository;
import com.bankscheduling.appointment.repository.RoleRepository;
import com.bankscheduling.appointment.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import com.bankscheduling.appointment.security.jwt.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.time.Duration;

@Service
public class CustomerAuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AppointmentRepository appointmentRepository;
    private final RoleRepository roleRepository;
    private final BranchBusinessHoursRepository branchBusinessHoursRepository;
    private final AppointmentSlotInventoryService appointmentSlotInventoryService;

    public CustomerAuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            AppointmentRepository appointmentRepository,
            RoleRepository roleRepository,
            BranchBusinessHoursRepository branchBusinessHoursRepository,
            AppointmentSlotInventoryService appointmentSlotInventoryService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.appointmentRepository = appointmentRepository;
        this.roleRepository = roleRepository;
        this.branchBusinessHoursRepository = branchBusinessHoursRepository;
        this.appointmentSlotInventoryService = appointmentSlotInventoryService;
    }

    @Transactional
    public CustomerAuthResponse register(CustomerRegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.findByEmailNormalized(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with that email already exists");
        }

        User user = new User();
        user.setEmailNormalized(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(resolveRoleOrThrow("ROLE_CUSTOMER"));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setFullName(request.firstName().trim() + " " + request.lastName().trim());
        userRepository.save(user);

        String token = jwtTokenProvider.createAccessToken(user.getId(), List.of(user.getRole().getName()));
        return new CustomerAuthResponse(token, toCustomerProfile(user));
    }

    @Transactional(readOnly = true)
    public CustomerAuthResponse login(CustomerAuthRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User account = userRepository.findByEmailNormalized(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!account.isActive() || !passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = jwtTokenProvider.createAccessToken(account.getId(), List.of(account.getRole().getName()));
        return new CustomerAuthResponse(token, toCustomerProfile(account));
    }

    @Transactional(readOnly = true)
    public CustomerProfileDto getCurrentCustomerProfile() {
        User account = getCurrentAccount();
        return toCustomerProfile(account);
    }

    @Transactional(readOnly = true)
    public List<CustomerAppointmentDto> getCurrentCustomerAppointments(String statusFilter) {
        User account = getCurrentAccount();
        Long customerId = account.getId();
        AppointmentStatus status = parseOptionalStatus(statusFilter);
        return appointmentRepository.findAllByCustomerIdAndOptionalStatus(customerId, status).stream()
                .map(appointment -> new CustomerAppointmentDto(
                        appointment.getId(),
                        appointment.getServiceType().getDisplayName(),
                        appointment.getBranch().getDisplayName(),
                        appointment.getScheduledStart(),
                        appointment.getScheduledEnd(),
                        appointment.getStatus().name()
                ))
                .toList();
    }

    @Transactional
    public CustomerAppointmentDto updateCurrentCustomerAppointment(Long appointmentId, CustomerAppointmentUpdateRequest request) {
        User account = getCurrentAccount();
        Appointment appointment = appointmentRepository.findByIdWithAssociations(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        if (!appointment.getCustomer().getId().equals(account.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update your own appointments");
        }
        if (appointment.getStatus() != AppointmentStatus.REQUESTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only requested appointments can be edited");
        }

        ZoneId branchZone = ZoneId.of(appointment.getBranch().getTimeZone());
        ZonedDateTime start = ZonedDateTime.of(request.date(), request.startTime(), branchZone);
        int duration = (int) Duration.between(appointment.getScheduledStart(), appointment.getScheduledEnd()).toMinutes();
        if (duration < 30 || duration % 30 != 0) {
            duration = appointment.getServiceType().getDefaultDurationMinutes();
        }
        ZonedDateTime end = start.plusMinutes(duration);
        Instant startInstant = start.toInstant();
        Instant endInstant = end.toInstant();

        validateNineToFiveWindow(request.startTime(), end.toLocalTime());
        validateSlotBoundary(request.startTime(), duration);
        BranchBusinessHours hours = branchBusinessHoursRepository
                .findByBranchIdAndDayOfWeekAndActiveTrue(
                        appointment.getBranch().getId(),
                        request.date().getDayOfWeek().getValue()
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch is closed on the selected date"));
        if (request.startTime().isBefore(hours.getOpenTime()) || end.toLocalTime().isAfter(hours.getCloseTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment is outside branch business hours");
        }
        if (startInstant.isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot book an appointment in the past");
        }
        if (appointmentRepository.existsActiveBranchServiceOverlapExcluding(
                appointment.getBranch().getId(),
                appointment.getServiceType().getId(),
                appointment.getId(),
                startInstant,
                endInstant
        )) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected timeslot is no longer available");
        }
        if (appointmentRepository.existsEmployeeOverlapExcluding(
                appointment.getEmployee().getId(),
                appointment.getId(),
                startInstant,
                endInstant
        )) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected timeslot is no longer available");
        }

        appointment.setScheduledStart(startInstant);
        appointment.setScheduledEnd(endInstant);
        appointment.setNotes(request.notes());
        appointment.setStatus(AppointmentStatus.REQUESTED);
        Appointment saved;
        try {
            saved = appointmentRepository.save(appointment);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected timeslot is no longer available");
        }
        appointmentSlotInventoryService.releaseSlots(saved.getId());
        appointmentSlotInventoryService.reserveSlots(
                saved,
                saved.getBranch().getId(),
                saved.getServiceType().getId(),
                request.date(),
                request.startTime(),
                saved.getServiceType().getDefaultDurationMinutes()
        );
        return new CustomerAppointmentDto(
                saved.getId(),
                saved.getServiceType().getDisplayName(),
                saved.getBranch().getDisplayName(),
                saved.getScheduledStart(),
                saved.getScheduledEnd(),
                saved.getStatus().name()
        );
    }

    @Transactional
    public void cancelCurrentCustomerAppointment(Long appointmentId) {
        User account = getCurrentAccount();
        Appointment appointment = appointmentRepository.findByIdWithAssociations(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        if (!appointment.getCustomer().getId().equals(account.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only cancel your own appointments");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment saved = appointmentRepository.save(appointment);
        appointmentSlotInventoryService.releaseSlots(saved.getId());
    }

    private User getCurrentAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        Long accountId;
        try {
            accountId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication principal");
        }

        return userRepository.findByIdWithRole(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer account not found"));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private CustomerProfileDto toCustomerProfile(User account) {
        String fullName = account.getFullName() == null ? "" : account.getFullName().trim();
        String firstName = fullName;
        String lastName = "";
        int firstSpace = fullName.indexOf(' ');
        if (firstSpace > 0) {
            firstName = fullName.substring(0, firstSpace).trim();
            lastName = fullName.substring(firstSpace + 1).trim();
        }

        String role = account.getRole() == null ? "CUSTOMER" : normalizeRoleName(account.getRole().getName());

        return new CustomerProfileDto(
                account.getId(),
                firstName,
                lastName,
                fullName,
                account.getEmailNormalized(),
                role
        );
    }

    private Role resolveRoleOrThrow(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Required role is missing from database: " + roleName
                ));
    }

    private static String normalizeRoleName(String roleName) {
        if (roleName == null) {
            return "CUSTOMER";
        }
        return roleName.startsWith("ROLE_") ? roleName.substring("ROLE_".length()) : roleName;
    }

    private void validateNineToFiveWindow(LocalTime start, LocalTime end) {
        if (start.isBefore(LocalTime.of(9, 0)) || end.isAfter(LocalTime.of(17, 0))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointments must be between 9:00 AM and 5:00 PM");
        }
    }

    private void validateSlotBoundary(LocalTime startTime, int durationMinutes) {
        if (startTime.getSecond() != 0 || startTime.getNano() != 0 || startTime.getMinute() % 30 != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointments must start on 30-minute boundaries");
        }
        if (durationMinutes < 30 || durationMinutes % 30 != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service duration is not aligned with appointment slots");
        }
    }

    private AppointmentStatus parseOptionalStatus(String raw) {
        if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw)) {
            return null;
        }
        try {
            return AppointmentStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status filter");
        }
    }
}
