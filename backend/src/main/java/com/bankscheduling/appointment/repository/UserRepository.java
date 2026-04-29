package com.bankscheduling.appointment.repository;

import com.bankscheduling.appointment.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmailNormalized(String emailNormalized);

    @Query("select u from User u join fetch u.role where u.username = :username")
    Optional<User> findByUsernameForAuth(@Param("username") String username);

    @Query("select u from User u join fetch u.branch join fetch u.role where u.id = :id")
    Optional<User> findByIdWithBranchAndRole(@Param("id") Long id);

    @Query("select u from User u join fetch u.role where u.id = :id")
    Optional<User> findByIdWithRole(@Param("id") Long id);

    @Query("""
            select u from User u
            join fetch u.role
            order by u.id asc
            """)
    List<User> findAllWithRole();

    @Query("""
            select distinct u from User u
            join EmployeeServiceLink es on es.employee.id = u.id
            where u.active = true
              and u.branch.id = :branchId
              and es.serviceType.id = :serviceTypeId
              and (u.role.name = 'ROLE_EMPLOYEE' or u.role.name = 'ROLE_ADMIN')
            order by u.id asc
            """)
    List<User> findActiveStaffByBranchAndServiceType(
            @Param("branchId") Long branchId,
            @Param("serviceTypeId") Long serviceTypeId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select distinct u from User u
            join EmployeeServiceLink es on es.employee.id = u.id
            where u.active = true
              and u.branch.id = :branchId
              and es.serviceType.id = :serviceTypeId
              and (u.role.name = 'ROLE_EMPLOYEE' or u.role.name = 'ROLE_ADMIN')
            order by u.id asc
            """)
    List<User> findActiveStaffByBranchAndServiceTypeForUpdate(
            @Param("branchId") Long branchId,
            @Param("serviceTypeId") Long serviceTypeId
    );
}
