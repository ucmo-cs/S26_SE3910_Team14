package com.bankscheduling.appointment.repository;

import com.bankscheduling.appointment.entity.EmployeeServiceLink;
import com.bankscheduling.appointment.entity.EmployeeServiceLinkId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeServiceLinkRepository extends JpaRepository<EmployeeServiceLink, EmployeeServiceLinkId> {
}
