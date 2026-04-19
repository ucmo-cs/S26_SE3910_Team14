package com.bankscheduling.appointment.repository;

import com.bankscheduling.appointment.entity.ServerSideSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ServerSideSessionRepository extends JpaRepository<ServerSideSession, Long> {

    Optional<ServerSideSession> findByRefreshTokenHashAndRevokedAtIsNull(String refreshTokenHash);

    @Query("""
            select s from ServerSideSession s
            join fetch s.employee e
            join fetch e.role
            where s.id = :id and s.revokedAt is null
            """)
    Optional<ServerSideSession> findActiveByIdWithEmployee(@Param("id") Long id);
}
