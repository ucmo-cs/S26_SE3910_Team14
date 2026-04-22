package com.bankscheduling.appointment.repository;

import com.bankscheduling.appointment.entity.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceTypeRepository extends JpaRepository<ServiceType, Long> {
    List<ServiceType> findByActiveTrueOrderByDisplayNameAsc();

    Optional<ServiceType> findByIdAndActiveTrue(Long id);
}
