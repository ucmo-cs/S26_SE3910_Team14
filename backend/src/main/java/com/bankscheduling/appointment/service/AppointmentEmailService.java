package com.bankscheduling.appointment.service;

import com.bankscheduling.appointment.entity.Appointment;

import java.time.ZoneId;

public interface AppointmentEmailService {
    void sendBookingConfirmation(
            Appointment appointment,
            ZoneId branchZone,
            String recipientEmail,
            String recipientName
    );
}
