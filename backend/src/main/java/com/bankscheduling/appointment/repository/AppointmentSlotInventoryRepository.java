package com.bankscheduling.appointment.repository;

import com.bankscheduling.appointment.entity.AppointmentSlotInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentSlotInventoryRepository extends JpaRepository<AppointmentSlotInventory, Long> {

    @Modifying
    @Query(
            value = """
                    insert into appointment_slot_inventory (branch_id, service_type_id, slot_date, slot_start_time, appointment_id)
                    select :branchId, :serviceTypeId, :slotDate, (time '09:00' + (n * interval '30 minutes'))::time, null
                    from generate_series(0, 15) as n
                    on conflict (branch_id, service_type_id, slot_date, slot_start_time) do nothing
                    """,
            nativeQuery = true
    )
    void initializeDaySlots(
            @Param("branchId") Long branchId,
            @Param("serviceTypeId") Long serviceTypeId,
            @Param("slotDate") LocalDate slotDate
    );

    @Query("""
            select s from AppointmentSlotInventory s
            left join fetch s.appointment
            where s.branch.id = :branchId
              and s.serviceType.id = :serviceTypeId
              and s.slotDate = :slotDate
            order by s.slotStartTime asc
            """)
    List<AppointmentSlotInventory> findDaySlots(
            @Param("branchId") Long branchId,
            @Param("serviceTypeId") Long serviceTypeId,
            @Param("slotDate") LocalDate slotDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from AppointmentSlotInventory s
            left join fetch s.appointment
            where s.branch.id = :branchId
              and s.serviceType.id = :serviceTypeId
              and s.slotDate = :slotDate
              and s.slotStartTime in :slotStartTimes
            order by s.slotStartTime asc
            """)
    List<AppointmentSlotInventory> findDaySlotsForUpdate(
            @Param("branchId") Long branchId,
            @Param("serviceTypeId") Long serviceTypeId,
            @Param("slotDate") LocalDate slotDate,
            @Param("slotStartTimes") List<LocalTime> slotStartTimes
    );

    @Modifying
    @Query("""
            update AppointmentSlotInventory s
            set s.appointment = null
            where s.appointment.id = :appointmentId
            """)
    void clearByAppointmentId(@Param("appointmentId") Long appointmentId);
}
