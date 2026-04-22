package com.bankscheduling.appointment.repository;

import com.bankscheduling.appointment.entity.Employee;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByUsername(String username);
    Optional<Employee> findByWorkEmail(String workEmail);

    @Query("select e from Employee e join fetch e.role where e.username = :username")
    Optional<Employee> findByUsernameForAuth(@Param("username") String username);

    @Query("select e from Employee e join fetch e.branch join fetch e.role where e.id = :id")
    Optional<Employee> findByIdWithBranchAndRole(@Param("id") Long id);

    @Query("""
            select distinct e from Employee e
            join EmployeeServiceLink es on es.employee.id = e.id
            where e.active = true
              and e.branch.id = :branchId
              and es.serviceType.id = :serviceTypeId
            order by e.id asc
            """)
    List<Employee> findActiveByBranchAndServiceType(
            @Param("branchId") Long branchId,
            @Param("serviceTypeId") Long serviceTypeId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select distinct e from Employee e
            join EmployeeServiceLink es on es.employee.id = e.id
            where e.active = true
              and e.branch.id = :branchId
              and es.serviceType.id = :serviceTypeId
            order by e.id asc
            """)
    List<Employee> findActiveByBranchAndServiceTypeForUpdate(
            @Param("branchId") Long branchId,
            @Param("serviceTypeId") Long serviceTypeId
    );
}
