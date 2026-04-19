package com.bankscheduling.appointment.service;

import com.bankscheduling.appointment.entity.Employee;
import com.bankscheduling.appointment.entity.ServerSideSession;
import com.bankscheduling.appointment.repository.EmployeeRepository;
import com.bankscheduling.appointment.repository.ServerSideSessionRepository;
import com.bankscheduling.appointment.security.JwtCookieNames;
import com.bankscheduling.appointment.security.JwtSecurityProperties;
import com.bankscheduling.appointment.security.cookie.TokenCookieService;
import com.bankscheduling.appointment.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final ServerSideSessionRepository sessionRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtSecurityProperties jwtSecurityProperties;
    private final PasswordEncoder passwordEncoder;
    private final TokenCookieService tokenCookieService;

    public AuthService(
            EmployeeRepository employeeRepository,
            ServerSideSessionRepository sessionRepository,
            JwtTokenProvider jwtTokenProvider,
            JwtSecurityProperties jwtSecurityProperties,
            PasswordEncoder passwordEncoder,
            TokenCookieService tokenCookieService
    ) {
        this.employeeRepository = employeeRepository;
        this.sessionRepository = sessionRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtSecurityProperties = jwtSecurityProperties;
        this.passwordEncoder = passwordEncoder;
        this.tokenCookieService = tokenCookieService;
    }

    /**
     * Validates credentials, persists a {@link ServerSideSession}, and returns raw JWT strings for the controller
     * to bind as HttpOnly cookies (never included in JSON).
     */
    @Transactional
    public LoginTokenPair login(String username, String password, HttpServletRequest request) {
        Employee employee = employeeRepository.findByUsernameForAuth(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!employee.isActive() || employee.isAccountLocked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (!passwordEncoder.matches(password, employee.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        ServerSideSession session = new ServerSideSession();
        session.setEmployee(employee);
        session.setIpAddress(clientIp(request));
        session.setUserAgent(trimUserAgent(request.getHeader("User-Agent")));
        session.setExpiresAt(Instant.now().plusSeconds(jwtSecurityProperties.getRefreshTokenTtlSeconds()));
        session.setCreatedAt(Instant.now());
        session.setLastSeenAt(Instant.now());
        session.setRefreshTokenHash(hash(UUID.randomUUID().toString()));
        sessionRepository.save(session);

        String refreshPlain = jwtTokenProvider.createRefreshToken(employee.getId(), session.getId());
        session.setRefreshTokenHash(hash(refreshPlain));
        sessionRepository.save(session);

        List<String> roles = roleClaimsFor(employee);
        String accessPlain = jwtTokenProvider.createAccessToken(employee.getId(), roles);
        return new LoginTokenPair(accessPlain, refreshPlain);
    }

    /**
     * Validates the refresh JWT and active {@link ServerSideSession}, then returns a new access token string
     * for the controller to set as the access cookie.
     */
    @Transactional
    public String refreshAccessToken(HttpServletRequest request) {
        String refresh = readCookie(request, JwtCookieNames.REFRESH_TOKEN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing refresh session"));

        Claims claims;
        try {
            claims = jwtTokenProvider.parseAndValidate(refresh);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        if (!jwtTokenProvider.isRefreshToken(claims)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        long sessionId = jwtTokenProvider.extractSessionIdFromRefresh(claims);
        ServerSideSession session = sessionRepository.findActiveByIdWithEmployee(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session not found or revoked"));

        if (!session.getRefreshTokenHash().equals(hash(refresh))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session mismatch");
        }

        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired");
        }

        session.setLastSeenAt(Instant.now());
        sessionRepository.save(session);

        Employee employee = session.getEmployee();
        List<String> roles = roleClaimsFor(employee);
        return jwtTokenProvider.createAccessToken(employee.getId(), roles);
    }

    /**
     * Revokes the current refresh session (if any) and clears auth cookies.
     */
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        readCookie(request, JwtCookieNames.REFRESH_TOKEN).ifPresent(refresh -> {
            try {
                Claims claims = jwtTokenProvider.parseAndValidate(refresh);
                if (!jwtTokenProvider.isRefreshToken(claims)) {
                    return;
                }
                long sessionId = jwtTokenProvider.extractSessionIdFromRefresh(claims);
                sessionRepository.findActiveByIdWithEmployee(sessionId).ifPresent(session -> {
                    if (session.getRefreshTokenHash().equals(hash(refresh))) {
                        session.setRevokedAt(Instant.now());
                        sessionRepository.save(session);
                    }
                });
            } catch (RuntimeException ignored) {
                // still clear cookies
            }
        });
        tokenCookieService.clearTokenCookies(response);
    }

    public record LoginTokenPair(String accessToken, String refreshToken) {
    }

    private static List<String> roleClaimsFor(Employee employee) {
        String name = employee.getRole().getName();
        if (name == null || name.isBlank()) {
            return List.of("ROLE_EMPLOYEE");
        }
        if (name.startsWith("ROLE_")) {
            return List.of(name);
        }
        return List.of("ROLE_" + name);
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String trimUserAgent(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() > 512 ? userAgent.substring(0, 512) : userAgent;
    }

    private static Optional<String> readCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
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
