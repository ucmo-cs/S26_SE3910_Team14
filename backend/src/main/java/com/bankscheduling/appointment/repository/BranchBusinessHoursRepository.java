package com.bankscheduling.appointment.repository;

import com.bankscheduling.appointment.entity.BranchBusinessHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchBusinessHoursRepository extends JpaRepository<BranchBusinessHours, Long> {
    List<BranchBusinessHours> findByBranchIdAndActiveTrueOrderByDayOfWeekAsc(Long branchId);

    Optional<BranchBusinessHours> findByBranchIdAndDayOfWeekAndActiveTrue(Long branchId, int dayOfWeek);
}
