package com.bankscheduling.appointment.service;

import com.bankscheduling.appointment.entity.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

@Service
@ConditionalOnProperty(prefix = "scheduling.mail", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpAppointmentEmailService implements AppointmentEmailService {
    private static final Logger log = LoggerFactory.getLogger(NoOpAppointmentEmailService.class);

    @Override
    public void sendBookingConfirmation(Appointment appointment, ZoneId branchZone) {
        log.info("Email delivery not configured; skipping confirmation for appointment {}", appointment.getId());
    }
}
