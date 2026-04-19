package com.bankscheduling.appointment.repository;

import com.bankscheduling.appointment.entity.ServerSideSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServerSideSessionRepository extends JpaRepository<ServerSideSession, Long> {

    Optional<ServerSideSession> findByRefreshTokenHashAndRevokedAtIsNull(String refreshTokenHash);
}
