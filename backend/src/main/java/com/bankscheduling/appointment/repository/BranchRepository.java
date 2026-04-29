package com.bankscheduling.appointment.repository;

import com.bankscheduling.appointment.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByActiveTrueOrderByDisplayNameAsc();

    Optional<Branch> findByIdAndActiveTrue(Long id);

    @Query("""
            select distinct b from Branch b
            join User e on e.branch.id = b.id
            join EmployeeServiceLink es on es.employee.id = e.id
            where b.active = true
              and e.active = true
              and (e.role.name = 'ROLE_EMPLOYEE' or e.role.name = 'ROLE_ADMIN')
              and es.serviceType.id = :serviceTypeId
            order by b.displayName asc
            """)
    List<Branch> findActiveByServiceType(@Param("serviceTypeId") Long serviceTypeId);
}
