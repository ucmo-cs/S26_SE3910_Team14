package com.bankscheduling.appointment.repository;

import com.bankscheduling.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
            select a from Appointment a
            join fetch a.branch
            join fetch a.customer
            join fetch a.employee
            join fetch a.serviceType
            where a.branch.id = :branchId
            order by a.scheduledStart asc
            """)
    List<Appointment> findAllByBranchIdWithAssociationsOrdered(@Param("branchId") Long branchId);
}
