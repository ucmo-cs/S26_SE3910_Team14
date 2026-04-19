package com.bankscheduling.appointment.security.jwt;

import com.bankscheduling.appointment.security.JwtSecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Creates and validates JWT access and refresh tokens. Tokens are delivered to clients only via HttpOnly cookies.
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TYP = "typ";
    private static final String CLAIM_SID = "sid";
    private static final String CLAIM_ROLES = "roles";
    private static final String TYP_REFRESH = "refresh";

    private final JwtSecurityProperties properties;

    public JwtTokenProvider(JwtSecurityProperties properties) {
        this.properties = properties;
    }

    public String createAccessToken(long employeeId, Collection<String> roleNames) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getAccessTokenTtlSeconds());
        List<String> roles = roleNames == null ? List.of() : new ArrayList<>(roleNames);
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(String.valueOf(employeeId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_ROLES, roles)
                .signWith(signingKey())
                .compact();
    }

    public String createRefreshToken(long employeeId, long serverSessionId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getRefreshTokenTtlSeconds());
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(String.valueOf(employeeId))
                .claim(CLAIM_TYP, TYP_REFRESH)
                .claim(CLAIM_SID, String.valueOf(serverSessionId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .id(UUID.randomUUID().toString())
                .signWith(signingKey())
                .compact();
    }

    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isRefreshToken(Claims claims) {
        return TYP_REFRESH.equals(claims.get(CLAIM_TYP));
    }

    public long extractUserId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        Object raw = claims.get(CLAIM_ROLES);
        if (raw instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o != null) {
                    out.add(o.toString());
                }
            }
            return List.copyOf(out);
        }
        if (raw instanceof Collection<?> col) {
            return col.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    public long extractSessionIdFromRefresh(Claims refreshClaims) {
        Object sid = refreshClaims.get(CLAIM_SID);
        if (sid == null) {
            throw new IllegalArgumentException("Missing session id on refresh token");
        }
        return Long.parseLong(sid.toString());
    }

    private SecretKey signingKey() {
        String b64 = properties.getSigningKeyBase64();
        if (b64 == null || b64.isBlank()) {
            throw new IllegalStateException("JWT signing key missing: set scheduling.security.jwt.signing-key-base64");
        }
        byte[] bytes = Base64.getDecoder().decode(b64);
        return Keys.hmacShaKeyFor(bytes);
    }
}
