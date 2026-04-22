package com.bankscheduling.appointment.repository;

import com.bankscheduling.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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

    @Query("""
            select a from Appointment a
            join fetch a.branch
            join fetch a.serviceType
            where a.customer.id = :customerId
            order by a.scheduledStart asc
            """)
    List<Appointment> findAllByCustomerIdWithAssociationsOrdered(@Param("customerId") Long customerId);

    @Query("""
            select a from Appointment a
            join fetch a.branch
            join fetch a.customer
            join fetch a.employee
            join fetch a.serviceType
            order by a.scheduledStart desc
            """)
    List<Appointment> findAllWithAssociationsOrderByScheduledStartDesc();

    @Query("""
            select a from Appointment a
            join fetch a.branch
            join fetch a.customer
            join fetch a.employee
            join fetch a.serviceType
            where a.id = :appointmentId
            """)
    Optional<Appointment> findByIdWithAssociations(@Param("appointmentId") Long appointmentId);

    @Query("""
            select a from Appointment a
            where a.branch.id = :branchId
              and a.scheduledStart < :dayEnd
              and a.scheduledEnd > :dayStart
            order by a.scheduledStart asc
            """)
    List<Appointment> findAllByBranchAndWindow(
            @Param("branchId") Long branchId,
            @Param("dayStart") Instant dayStart,
            @Param("dayEnd") Instant dayEnd
    );

    @Query("""
            select a from Appointment a
            where a.branch.id = :branchId
              and a.serviceType.id = :serviceTypeId
              and a.scheduledStart >= :dayStart
              and a.scheduledStart < :dayEnd
              and a.status <> com.bankscheduling.appointment.entity.AppointmentStatus.CANCELLED
            """)
    List<Appointment> findActiveByBranchServiceAndDayWindow(
            @Param("branchId") Long branchId,
            @Param("serviceTypeId") Long serviceTypeId,
            @Param("dayStart") Instant dayStart,
            @Param("dayEnd") Instant dayEnd
    );

    @Query("""
            select count(a) > 0 from Appointment a
            where a.branch.id = :branchId
              and a.serviceType.id = :serviceTypeId
              and a.scheduledStart = :scheduledStart
              and a.status <> com.bankscheduling.appointment.entity.AppointmentStatus.CANCELLED
            """)
    boolean existsActiveBranchServiceStart(
            @Param("branchId") Long branchId,
            @Param("serviceTypeId") Long serviceTypeId,
            @Param("scheduledStart") Instant scheduledStart
    );

    @Query("""
            select count(a) > 0 from Appointment a
            where a.branch.id = :branchId
              and a.serviceType.id = :serviceTypeId
              and a.scheduledStart = :scheduledStart
              and a.id <> :appointmentId
              and a.status <> com.bankscheduling.appointment.entity.AppointmentStatus.CANCELLED
            """)
    boolean existsActiveBranchServiceStartExcluding(
            @Param("branchId") Long branchId,
            @Param("serviceTypeId") Long serviceTypeId,
            @Param("scheduledStart") Instant scheduledStart,
            @Param("appointmentId") Long appointmentId
    );

    @Query("""
            select count(a) > 0 from Appointment a
            where a.employee.id = :employeeId
              and a.status <> com.bankscheduling.appointment.entity.AppointmentStatus.CANCELLED
              and a.scheduledStart < :scheduledEnd
              and a.scheduledEnd > :scheduledStart
            """)
    boolean existsEmployeeOverlap(
            @Param("employeeId") Long employeeId,
            @Param("scheduledStart") Instant scheduledStart,
            @Param("scheduledEnd") Instant scheduledEnd
    );

    @Query("""
            select count(a) > 0 from Appointment a
            where a.employee.id = :employeeId
              and a.id <> :appointmentId
              and a.status <> com.bankscheduling.appointment.entity.AppointmentStatus.CANCELLED
              and a.scheduledStart < :scheduledEnd
              and a.scheduledEnd > :scheduledStart
            """)
    boolean existsEmployeeOverlapExcluding(
            @Param("employeeId") Long employeeId,
            @Param("appointmentId") Long appointmentId,
            @Param("scheduledStart") Instant scheduledStart,
            @Param("scheduledEnd") Instant scheduledEnd
    );

    @Query("""
            select a from Appointment a
            where a.employee.id in :employeeIds
              and a.status <> com.bankscheduling.appointment.entity.AppointmentStatus.CANCELLED
              and a.scheduledStart < :windowEnd
              and a.scheduledEnd > :windowStart
            order by a.scheduledStart asc
            """)
    List<Appointment> findEmployeeOverlapsInWindow(
            @Param("employeeIds") List<Long> employeeIds,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd
    );
}
