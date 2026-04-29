package com.bankscheduling.appointment.repository;

import com.bankscheduling.appointment.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GuestRepository extends JpaRepository<Guest, Long> {
    Optional<Guest> findByAppointmentId(Long appointmentId);

    List<Guest> findAllByAppointmentIdIn(Collection<Long> appointmentIds);
}
