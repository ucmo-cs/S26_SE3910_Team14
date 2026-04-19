package com.bankscheduling.appointment.repository;

import com.bankscheduling.appointment.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}
