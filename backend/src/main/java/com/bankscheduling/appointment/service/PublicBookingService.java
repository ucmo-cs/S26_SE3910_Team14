package com.bankscheduling.appointment.service;

import com.bankscheduling.appointment.dto.publicbooking.BookingRequestDto;
import com.bankscheduling.appointment.dto.publicbooking.BookingResponseDto;
import com.bankscheduling.appointment.dto.publicbooking.PublicBranchDto;
import com.bankscheduling.appointment.dto.publicbooking.PublicServiceTypeDto;
import com.bankscheduling.appointment.dto.publicbooking.PublicTimeslotsDto;
import com.bankscheduling.appointment.entity.Appointment;
import com.bankscheduling.appointment.entity.AppointmentStatus;
import com.bankscheduling.appointment.entity.AppointmentSlotInventory;
import com.bankscheduling.appointment.entity.Branch;
import com.bankscheduling.appointment.entity.BranchBusinessHours;
import com.bankscheduling.appointment.entity.Customer;
import com.bankscheduling.appointment.entity.CustomerAccount;
import com.bankscheduling.appointment.entity.Employee;
import com.bankscheduling.appointment.entity.ServiceType;
import com.bankscheduling.appointment.repository.AppointmentRepository;
import com.bankscheduling.appointment.repository.BranchBusinessHoursRepository;
import com.bankscheduling.appointment.repository.BranchRepository;
import com.bankscheduling.appointment.repository.CustomerAccountRepository;
import com.bankscheduling.appointment.repository.CustomerRepository;
import com.bankscheduling.appointment.repository.EmployeeRepository;
import com.bankscheduling.appointment.repository.ServiceTypeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PublicBookingService {
    private static final int BASE_SLOT_MINUTES = 30;
    private static final LocalTime DEMO_OPEN_TIME = LocalTime.of(9, 0);
    private static final LocalTime DEMO_CLOSE_TIME = LocalTime.of(17, 0);
    private static final DateTimeFormatter SLOT_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final ServiceTypeRepository serviceTypeRepository;
    private final BranchRepository branchRepository;
    private final AppointmentRepository appointmentRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final CustomerAccountRepository customerAccountRepository;
    private final BranchBusinessHoursRepository branchBusinessHoursRepository;
    private final AppointmentEmailService appointmentEmailService;
    private final AppointmentSlotInventoryService appointmentSlotInventoryService;

    public PublicBookingService(
            ServiceTypeRepository serviceTypeRepository,
            BranchRepository branchRepository,
            AppointmentRepository appointmentRepository,
            EmployeeRepository employeeRepository,
            CustomerRepository customerRepository,
            CustomerAccountRepository customerAccountRepository,
            BranchBusinessHoursRepository branchBusinessHoursRepository,
            AppointmentEmailService appointmentEmailService,
            AppointmentSlotInventoryService appointmentSlotInventoryService
    ) {
        this.serviceTypeRepository = serviceTypeRepository;
        this.branchRepository = branchRepository;
        this.appointmentRepository = appointmentRepository;
        this.employeeRepository = employeeRepository;
        this.customerRepository = customerRepository;
        this.customerAccountRepository = customerAccountRepository;
        this.branchBusinessHoursRepository = branchBusinessHoursRepository;
        this.appointmentEmailService = appointmentEmailService;
        this.appointmentSlotInventoryService = appointmentSlotInventoryService;
    }

    @Transactional(readOnly = true)
    public List<PublicServiceTypeDto> getTopics() {
        return serviceTypeRepository.findByActiveTrueOrderByDisplayNameAsc().stream()
                .map(topic -> new PublicServiceTypeDto(
                        topic.getId(),
                        topic.getCode(),
                        topic.getDisplayName(),
                        topic.getDescription(),
                        topic.getDefaultDurationMinutes()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicBranchDto> getBranches(Long topicId) {
        List<Branch> branches = topicId == null
                ? branchRepository.findByActiveTrueOrderByDisplayNameAsc()
                : branchRepository.findActiveByServiceType(topicId);
        return branches.stream().map(this::toBranchDto).toList();
    }

    @Transactional(readOnly = true)
    public PublicTimeslotsDto getAvailableTimes(Long branchId, Long topicId, LocalDate date, int slotStepMinutes) {
        validateSlotStep(slotStepMinutes);
        Branch branch = findActiveBranch(branchId);
        ServiceType topic = findActiveTopic(topicId);
        ensureTopicCanBeBookedAtBranch(branch, topic);

        ZoneId branchZone = zoneFor(branch);
        BranchBusinessHours hours = findBusinessHours(branchId, date);
        List<Employee> eligibleEmployees = employeeRepository.findActiveByBranchAndServiceType(branchId, topicId);
        LocalTime openTime = effectiveOpenTime(hours);
        LocalTime closeTime = effectiveCloseTime(hours);
        List<String> allSlots = buildCandidateSlots(openTime, closeTime, slotStepMinutes);

        ZonedDateTime openAt = dateTimeAtZone(date, openTime, branchZone);
        ZonedDateTime closeAt = dateTimeAtZone(date, closeTime, branchZone);
        Instant dayStart = openAt.toInstant();
        Instant dayEnd = closeAt.toInstant();
        List<Long> employeeIds = eligibleEmployees.stream().map(Employee::getId).toList();
        List<Appointment> employeeAppointments = employeeIds.isEmpty()
                ? List.of()
                : appointmentRepository.findEmployeeOverlapsInWindow(employeeIds, dayStart, dayEnd);
        List<AppointmentSlotInventory> daySlots = appointmentSlotInventoryService.getDaySlots(branchId, topicId, date);
        java.util.Map<LocalTime, AppointmentSlotInventory> slotByTime = daySlots.stream()
                .collect(Collectors.toMap(AppointmentSlotInventory::getSlotStartTime, s -> s));

        List<String> available = new ArrayList<>();
        for (String candidate : allSlots) {
            LocalTime startTime = LocalTime.parse(candidate, SLOT_FORMAT);
            LocalDateTime slotCursor = LocalDateTime.of(date, startTime);
            ZonedDateTime slotStart = slotCursor.atZone(branchZone);
            ZonedDateTime slotEnd = slotStart.plusMinutes(topic.getDefaultDurationMinutes());
            Instant slotStartInstant = slotStart.toInstant();
            Instant slotEndInstant = slotEnd.toInstant();

            if (slotEnd.toLocalTime().isAfter(closeTime)) {
                continue;
            }
            boolean slotInventoryFree = isSlotInventoryFree(slotByTime, startTime, topic.getDefaultDurationMinutes());
            boolean slotHasEmployee = employeeIds.stream().anyMatch(employeeId ->
                    employeeAppointments.stream()
                            .filter(a -> a.getEmployee().getId().equals(employeeId))
                            .noneMatch(appointment ->
                                    appointment.getScheduledStart().isBefore(slotEndInstant)
                                            && appointment.getScheduledEnd().isAfter(slotStartInstant)
                            ));
            if (slotInventoryFree && slotHasEmployee) {
                available.add(slotStart.toLocalTime().format(SLOT_FORMAT));
            }
        }

        List<String> unavailable = allSlots.stream().filter(slot -> !available.contains(slot)).toList();
        return new PublicTimeslotsDto(branchZone.getId(), slotStepMinutes, available, unavailable);
    }

    @Transactional
    public BookingResponseDto book(BookingRequestDto request) {
        Branch branch = findActiveBranch(request.branchId());
        ServiceType topic = findActiveTopic(request.serviceTypeId());
        ensureTopicCanBeBookedAtBranch(branch, topic);

        ZoneId branchZone = zoneFor(branch);
        validateSlotBoundaries(request.startTime(), topic.getDefaultDurationMinutes());
        BranchBusinessHours hours = findBusinessHours(branch.getId(), request.date());

        ZonedDateTime slotStart = dateTimeAtZone(request.date(), request.startTime(), branchZone);
        ZonedDateTime slotEnd = slotStart.plusMinutes(topic.getDefaultDurationMinutes());
        validateWithinBusinessHours(slotStart.toLocalTime(), slotEnd.toLocalTime(), hours);

        Instant scheduledStart = slotStart.toInstant();
        Instant scheduledEnd = slotEnd.toInstant();
        if (scheduledStart.isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot book an appointment in the past");
        }
        List<Employee> eligibleEmployees = employeeRepository.findActiveByBranchAndServiceTypeForUpdate(branch.getId(), topic.getId());
        if (eligibleEmployees.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No specialists are currently available for this service");
        }

        Employee assignedEmployee = eligibleEmployees.stream()
                .filter(employee -> !appointmentRepository.existsEmployeeOverlap(employee.getId(), scheduledStart, scheduledEnd))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Selected timeslot is no longer available"));

        Customer customer = resolveCustomerForBooking(branch, request);

        Appointment appointment = new Appointment();
        appointment.setBranch(branch);
        appointment.setCustomer(customer);
        appointment.setEmployee(assignedEmployee);
        appointment.setServiceType(topic);
        appointment.setScheduledStart(scheduledStart);
        appointment.setScheduledEnd(scheduledEnd);
        appointment.setStatus(AppointmentStatus.REQUESTED);

        Appointment saved;
        try {
            saved = appointmentRepository.save(appointment);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected timeslot is no longer available");
        }
        appointmentSlotInventoryService.reserveSlots(
                saved,
                branch.getId(),
                topic.getId(),
                request.date(),
                request.startTime(),
                topic.getDefaultDurationMinutes()
        );
        appointmentEmailService.sendBookingConfirmation(saved, branchZone);
        return new BookingResponseDto(saved.getId(), "Appointment booked successfully");
    }

    private Customer resolveCustomerForBooking(Branch branch, BookingRequestDto request) {
        String fullName = request.firstName().trim() + " " + request.lastName().trim();
        String normalizedEmail = request.email().trim().toLowerCase();
        Optional<Long> accountId = getAuthenticatedCustomerAccountId();

        if (accountId.isPresent()) {
            CustomerAccount account = customerAccountRepository.findByIdWithCustomer(accountId.get())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer account not found"));
            Customer customer = account.getCustomer();
            customer.setBranch(branch);
            customer.setFullName(fullName);
            customer.setEmail(normalizedEmail);
            return customerRepository.save(customer);
        }

        Customer customer = new Customer();
        customer.setBranch(branch);
        customer.setFullName(fullName);
        customer.setEmail(normalizedEmail);
        return customerRepository.save(customer);
    }

    private PublicBranchDto toBranchDto(Branch branch) {
        return new PublicBranchDto(
                branch.getId(),
                branch.getDisplayName(),
                branch.getStreetLine1(),
                branch.getStreetLine2(),
                branch.getCity(),
                branch.getStateProvince(),
                branch.getPostalCode(),
                branch.getTimeZone()
        );
    }

    private Branch findActiveBranch(Long branchId) {
        return branchRepository.findByIdAndActiveTrue(branchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    }

    private ServiceType findActiveTopic(Long topicId) {
        return serviceTypeRepository.findByIdAndActiveTrue(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service type not found"));
    }

    private BranchBusinessHours findBusinessHours(Long branchId, LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        if (dayOfWeek > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointments are available Monday through Friday only");
        }
        return branchBusinessHoursRepository.findByBranchIdAndDayOfWeekAndActiveTrue(branchId, dayOfWeek)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch is closed on the selected date"));
    }

    private void ensureTopicCanBeBookedAtBranch(Branch branch, ServiceType topic) {
        List<Branch> availableBranches = branchRepository.findActiveByServiceType(topic.getId());
        boolean supports = availableBranches.stream().map(Branch::getId).anyMatch(branch.getId()::equals);
        if (!supports) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service type is not available for this branch");
        }
    }

    private void validateSlotBoundaries(LocalTime startTime, int durationMinutes) {
        if (startTime.getSecond() != 0 || startTime.getNano() != 0 || startTime.getMinute() % BASE_SLOT_MINUTES != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointments must start on 30-minute boundaries");
        }
        if (durationMinutes < BASE_SLOT_MINUTES || durationMinutes % BASE_SLOT_MINUTES != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service duration is not aligned with appointment slots");
        }
    }

    private void validateWithinBusinessHours(LocalTime slotStart, LocalTime slotEnd, BranchBusinessHours hours) {
        LocalTime open = effectiveOpenTime(hours);
        LocalTime close = effectiveCloseTime(hours);
        if (slotStart.isBefore(open) || slotEnd.isAfter(close)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment is outside branch business hours");
        }
    }

    private LocalTime effectiveOpenTime(BranchBusinessHours hours) {
        return hours.getOpenTime().isAfter(DEMO_OPEN_TIME) ? hours.getOpenTime() : DEMO_OPEN_TIME;
    }

    private LocalTime effectiveCloseTime(BranchBusinessHours hours) {
        return hours.getCloseTime().isBefore(DEMO_CLOSE_TIME) ? hours.getCloseTime() : DEMO_CLOSE_TIME;
    }

    private List<String> buildCandidateSlots(LocalTime openTime, LocalTime closeTime, int slotStepMinutes) {
        List<String> slots = new ArrayList<>();
        LocalTime cursor = openTime;
        LocalTime latestStart = closeTime.minusMinutes(slotStepMinutes);
        while (!cursor.isAfter(latestStart)) {
            slots.add(cursor.format(SLOT_FORMAT));
            cursor = cursor.plusMinutes(slotStepMinutes);
        }
        return slots;
    }

    private void validateSlotStep(int slotStepMinutes) {
        if (slotStepMinutes != 30 && slotStepMinutes != 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slot interval must be either 30 or 60 minutes");
        }
    }

    private boolean isSlotInventoryFree(
            java.util.Map<LocalTime, AppointmentSlotInventory> slotByTime,
            LocalTime startTime,
            int durationMinutes
    ) {
        int requiredSlots = durationMinutes / BASE_SLOT_MINUTES;
        LocalTime cursor = startTime;
        for (int i = 0; i < requiredSlots; i++) {
            AppointmentSlotInventory slot = slotByTime.get(cursor);
            if (slot == null || slot.getAppointment() != null) {
                return false;
            }
            cursor = cursor.plusMinutes(BASE_SLOT_MINUTES);
        }
        return true;
    }

    private ZoneId zoneFor(Branch branch) {
        try {
            return ZoneId.of(branch.getTimeZone());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid branch timezone configuration");
        }
    }

    private ZonedDateTime dateTimeAtZone(LocalDate date, LocalTime time, ZoneId zoneId) {
        return ZonedDateTime.of(date, time, zoneId);
    }

    private Optional<Long> getAuthenticatedCustomerAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        boolean isCustomer = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CUSTOMER".equals(authority.getAuthority()));
        if (!isCustomer) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.parseLong(authentication.getName()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
