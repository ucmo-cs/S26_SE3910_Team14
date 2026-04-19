package com.bankscheduling.appointment.repository;

import com.bankscheduling.appointment.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
