package com.bankscheduling.appointment.service;

import com.bankscheduling.appointment.entity.Appointment;
import com.bankscheduling.appointment.entity.AppointmentSlotInventory;
import com.bankscheduling.appointment.repository.AppointmentSlotInventoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AppointmentSlotInventoryService {

    private static final LocalTime SLOT_DAY_START = LocalTime.of(9, 0);
    private static final LocalTime SLOT_DAY_END = LocalTime.of(17, 0);
    private static final int SLOT_MINUTES = 30;

    private final AppointmentSlotInventoryRepository appointmentSlotInventoryRepository;

    public AppointmentSlotInventoryService(AppointmentSlotInventoryRepository appointmentSlotInventoryRepository) {
        this.appointmentSlotInventoryRepository = appointmentSlotInventoryRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<AppointmentSlotInventory> getDaySlots(Long branchId, Long serviceTypeId, LocalDate date) {
        appointmentSlotInventoryRepository.initializeDaySlots(branchId, serviceTypeId, date);
        return appointmentSlotInventoryRepository.findDaySlots(branchId, serviceTypeId, date);
    }

    @Transactional
    public void reserveSlots(
            Appointment appointment,
            Long branchId,
            Long serviceTypeId,
            LocalDate date,
            LocalTime startTime,
            int durationMinutes
    ) {
        List<LocalTime> requiredStarts = computeRequiredSlotStarts(startTime, durationMinutes);
        appointmentSlotInventoryRepository.initializeDaySlots(branchId, serviceTypeId, date);
        List<AppointmentSlotInventory> rows = appointmentSlotInventoryRepository.findDaySlotsForUpdate(
                branchId,
                serviceTypeId,
                date,
                requiredStarts
        );
        if (rows.size() != requiredStarts.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected timeslot is no longer available");
        }
        Set<LocalTime> actualStarts = new HashSet<>();
        for (AppointmentSlotInventory row : rows) {
            actualStarts.add(row.getSlotStartTime());
            if (row.getAppointment() != null && !row.getAppointment().getId().equals(appointment.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected timeslot is no longer available");
            }
        }
        for (LocalTime required : requiredStarts) {
            if (!actualStarts.contains(required)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected timeslot is no longer available");
            }
        }
        for (AppointmentSlotInventory row : rows) {
            row.setAppointment(appointment);
        }
        appointmentSlotInventoryRepository.saveAll(rows);
    }

    @Transactional
    public void releaseSlots(Long appointmentId) {
        appointmentSlotInventoryRepository.clearByAppointmentId(appointmentId);
    }

    private List<LocalTime> computeRequiredSlotStarts(LocalTime startTime, int durationMinutes) {
        if (durationMinutes < SLOT_MINUTES || durationMinutes % SLOT_MINUTES != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid appointment duration");
        }
        if (startTime.getSecond() != 0 || startTime.getNano() != 0 || startTime.getMinute() % SLOT_MINUTES != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointments must start on 30-minute boundaries");
        }
        int slots = durationMinutes / SLOT_MINUTES;
        List<LocalTime> starts = new ArrayList<>();
        LocalTime cursor = startTime;
        for (int i = 0; i < slots; i++) {
            starts.add(cursor);
            cursor = cursor.plusMinutes(SLOT_MINUTES);
        }
        if (startTime.isBefore(SLOT_DAY_START) || cursor.isAfter(SLOT_DAY_END)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment is outside branch business hours");
        }
        return starts;
    }
}
