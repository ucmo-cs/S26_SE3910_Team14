package com.bankscheduling.appointment.repository;

import com.bankscheduling.appointment.entity.CustomerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, Long> {
    Optional<CustomerAccount> findByEmailNormalized(String emailNormalized);

    @Query("select ca from CustomerAccount ca join fetch ca.customer join fetch ca.role where ca.id = :id")
    Optional<CustomerAccount> findByIdWithCustomer(@Param("id") Long id);

    @Query("""
            select ca from CustomerAccount ca
            join fetch ca.customer
            join fetch ca.role
            order by ca.id asc
            """)
    java.util.List<CustomerAccount> findAllWithCustomerAndRole();
}
