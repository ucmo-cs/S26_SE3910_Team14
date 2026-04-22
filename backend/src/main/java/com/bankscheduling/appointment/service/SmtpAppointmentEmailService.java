package com.bankscheduling.appointment.service;

import com.bankscheduling.appointment.entity.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@ConditionalOnProperty(prefix = "scheduling.mail", name = "enabled", havingValue = "true")
public class SmtpAppointmentEmailService implements AppointmentEmailService {
    private static final Logger log = LoggerFactory.getLogger(SmtpAppointmentEmailService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a z");

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpAppointmentEmailService(
            JavaMailSender mailSender,
            @Value("${scheduling.mail.from-address}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendBookingConfirmation(Appointment appointment, ZoneId branchZone) {
        var customer = appointment.getCustomer();
        var branch = appointment.getBranch();
        var topic = appointment.getServiceType();
        var start = appointment.getScheduledStart().atZone(branchZone);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(customer.getEmail());
        message.setSubject("Central Bank Appointment Confirmation");
        message.setText("""
                Hello %s,

                Your appointment has been confirmed.

                Confirmation ID: %s
                Topic: %s
                Branch: %s
                Date: %s
                Time: %s

                Thank you for choosing Central Bank.
                """.formatted(
                customer.getFullName(),
                appointment.getId(),
                topic.getDisplayName(),
                branch.getDisplayName(),
                start.format(DATE_FORMAT),
                start.format(TIME_FORMAT)
        ));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            // Booking is already persisted; email failures should not rollback appointment creation.
            log.error("Failed to send confirmation email for appointment {}", appointment.getId(), ex);
        }
    }
}
