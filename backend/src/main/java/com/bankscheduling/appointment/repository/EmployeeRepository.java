package com.bankscheduling.appointment.repository;

import com.bankscheduling.appointment.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByUsername(String username);

    @Query("select e from Employee e join fetch e.role where e.username = :username")
    Optional<Employee> findByUsernameForAuth(@Param("username") String username);
}
