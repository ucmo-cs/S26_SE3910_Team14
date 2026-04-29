package com.bankscheduling.appointment.service;

import com.bankscheduling.appointment.dto.publicbooking.PublicBranchDto;
import com.bankscheduling.appointment.dto.publicbooking.PublicTimeslotsDto;
import com.bankscheduling.appointment.entity.AppointmentSlotInventory;
import com.bankscheduling.appointment.entity.Branch;
import com.bankscheduling.appointment.entity.BranchBusinessHours;
import com.bankscheduling.appointment.entity.Employee;
import com.bankscheduling.appointment.entity.ServiceType;
import com.bankscheduling.appointment.repository.AppointmentRepository;
import com.bankscheduling.appointment.repository.BranchBusinessHoursRepository;
import com.bankscheduling.appointment.repository.BranchRepository;
import com.bankscheduling.appointment.repository.CustomerAccountRepository;
import com.bankscheduling.appointment.repository.CustomerRepository;
import com.bankscheduling.appointment.repository.EmployeeRepository;
import com.bankscheduling.appointment.repository.ServiceTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicBookingServiceTest {

    @Mock
    private ServiceTypeRepository serviceTypeRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CustomerAccountRepository customerAccountRepository;
    @Mock
    private BranchBusinessHoursRepository branchBusinessHoursRepository;
    @Mock
    private AppointmentEmailService appointmentEmailService;
    @Mock
    private AppointmentSlotInventoryService appointmentSlotInventoryService;

    private PublicBookingService publicBookingService;

    @BeforeEach
    void setUp() {
        publicBookingService = new PublicBookingService(
                serviceTypeRepository,
                branchRepository,
                appointmentRepository,
                employeeRepository,
                customerRepository,
                customerAccountRepository,
                branchBusinessHoursRepository,
                appointmentEmailService,
                appointmentSlotInventoryService
        );
    }

    @Test
    void getBranchesFiltersByTopic() {
        Branch branch = buildBranch(5L, "Downtown Branch");
        when(branchRepository.findActiveByServiceType(99L)).thenReturn(List.of(branch));

        List<PublicBranchDto> result = publicBookingService.getBranches(99L);

        assertEquals(1, result.size());
        assertEquals("Downtown Branch", result.get(0).displayName());
        verify(branchRepository).findActiveByServiceType(99L);
    }

    @Test
    void getAvailableTimesReturnsSlotsForOpenDay() {
        Branch branch = buildBranch(1L, "Main Branch");
        ServiceType topic = new ServiceType();
        topic.setId(10L);
        topic.setDisplayName("General Banking");
        topic.setDefaultDurationMinutes(30);
        topic.setActive(true);

        BranchBusinessHours mondayHours = new BranchBusinessHours();
        mondayHours.setBranch(branch);
        mondayHours.setDayOfWeek(1);
        mondayHours.setOpenTime(LocalTime.of(8, 0));
        mondayHours.setCloseTime(LocalTime.of(9, 0));
        mondayHours.setActive(true);

        Employee employee = new Employee();
        employee.setId(50L);

        LocalDate date = LocalDate.of(2026, 4, 27);
        when(branchRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(branch));
        when(serviceTypeRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(topic));
        when(branchRepository.findActiveByServiceType(10L)).thenReturn(List.of(branch));
        when(branchBusinessHoursRepository.findByBranchIdAndDayOfWeekAndActiveTrue(1L, 1)).thenReturn(Optional.of(mondayHours));
        when(employeeRepository.findActiveByBranchAndServiceType(1L, 10L)).thenReturn(List.of(employee));
        when(appointmentSlotInventoryService.getDaySlots(1L, 10L, date)).thenReturn(List.of(
                buildOpenSlot(branch, topic, date, LocalTime.of(9, 0)),
                buildOpenSlot(branch, topic, date, LocalTime.of(9, 30))
        ));

        PublicTimeslotsDto result = publicBookingService.getAvailableTimes(1L, 10L, date, 30);

        assertEquals("America/Chicago", result.timeZone());
        assertEquals(30, result.slotDurationMinutes());
        assertEquals(2, result.slots().size());
        assertEquals("09:00", result.slots().get(0));
        assertEquals("09:30", result.slots().get(1));
        assertFalse(result.slots().contains("10:00"));
    }

    private Branch buildBranch(Long id, String name) {
        Branch branch = new Branch();
        branch.setId(id);
        branch.setDisplayName(name);
        branch.setActive(true);
        branch.setTimeZone("America/Chicago");
        return branch;
    }

    private AppointmentSlotInventory buildOpenSlot(Branch branch, ServiceType topic, LocalDate date, LocalTime startTime) {
        AppointmentSlotInventory slot = new AppointmentSlotInventory();
        slot.setBranch(branch);
        slot.setServiceType(topic);
        slot.setSlotDate(date);
        slot.setSlotStartTime(startTime);
        return slot;
    }
}
