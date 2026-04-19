package com.bankscheduling.appointment.security.session;

import com.bankscheduling.appointment.entity.Employee;
import com.bankscheduling.appointment.entity.ServerSideSession;
import com.bankscheduling.appointment.repository.ServerSideSessionRepository;
import com.bankscheduling.appointment.security.JwtSecurityProperties;
import com.bankscheduling.appointment.security.cookie.TokenCookieService;
import com.bankscheduling.appointment.security.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Binds refresh JWTs to {@link ServerSideSession} rows for revocation, IP binding, and rotation.
 * Access/refresh token strings are never returned from REST controllers—only set as HttpOnly cookies.
 */
@Service
public class RefreshTokenSessionService {

    private final ServerSideSessionRepository sessionRepository;
    private final JwtService jwtService;
    private final JwtSecurityProperties jwtSecurityProperties;
    private final TokenCookieService tokenCookieService;

    public RefreshTokenSessionService(
            ServerSideSessionRepository sessionRepository,
            JwtService jwtService,
            JwtSecurityProperties jwtSecurityProperties,
            TokenCookieService tokenCookieService
    ) {
        this.sessionRepository = sessionRepository;
        this.jwtService = jwtService;
        this.jwtSecurityProperties = jwtSecurityProperties;
        this.tokenCookieService = tokenCookieService;
    }

    /**
     * Creates a server-side session, issues JWTs, and attaches cookies to the response.
     */
    public void establishSession(HttpServletRequest request, HttpServletResponse response, Employee employee) {
        ServerSideSession session = new ServerSideSession();
        session.setEmployee(employee);
        session.setIpAddress(clientIp(request));
        session.setUserAgent(request.getHeader("User-Agent"));
        session.setExpiresAt(Instant.now().plusSeconds(jwtSecurityProperties.getRefreshTokenTtlSeconds()));
        session.setCreatedAt(Instant.now());
        session.setLastSeenAt(Instant.now());
        session.setRefreshTokenHash(hash(UUID.randomUUID().toString()));
        sessionRepository.save(session);

        String refreshPlain = jwtService.createRefreshToken(employee.getId(), String.valueOf(session.getId()));
        session.setRefreshTokenHash(hash(refreshPlain));
        sessionRepository.save(session);

        String access = jwtService.createAccessToken(employee.getId(), java.util.Map.of());
        tokenCookieService.attachTokenCookies(response, access, refreshPlain);
    }

    /**
     * Skeleton for refresh flow: validate refresh cookie + DB row, rotate refresh token, re-issue cookies.
     */
    public void rotateRefreshToken(HttpServletRequest request, HttpServletResponse response, String refreshToken) {
        // TODO: parse JWT, verify typ=refresh, load session by id + hash, check revocation/expiry/IP binding, then re-issue cookies.
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
