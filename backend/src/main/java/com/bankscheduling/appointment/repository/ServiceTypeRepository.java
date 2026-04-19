package com.bankscheduling.appointment.repository;

import com.bankscheduling.appointment.entity.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceTypeRepository extends JpaRepository<ServiceType, Long> {
}
