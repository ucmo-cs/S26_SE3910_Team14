package com.bankscheduling.appointment.security.jwt;

import com.bankscheduling.appointment.security.JwtSecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Issues and parses JWTs. Tokens are only delivered to clients via {@link com.bankscheduling.appointment.security.cookie.TokenCookieService}.
 */
@Service
public class JwtService {

    private final JwtSecurityProperties properties;

    public JwtService(JwtSecurityProperties properties) {
        this.properties = properties;
    }

    public String createAccessToken(long employeeId, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getAccessTokenTtlSeconds());
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(String.valueOf(employeeId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .id(UUID.randomUUID().toString())
                .claims(extraClaims)
                .signWith(signingKey())
                .compact();
    }

    public String createRefreshToken(long employeeId, String sessionId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getRefreshTokenTtlSeconds());
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(String.valueOf(employeeId))
                .claim("typ", "refresh")
                .claim("sid", sessionId)
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

    private SecretKey signingKey() {
        String b64 = properties.getSigningKeyBase64();
        if (b64 == null || b64.isBlank()) {
            throw new IllegalStateException("JWT signing key missing: set scheduling.security.jwt.signing-key-base64");
        }
        byte[] bytes = Base64.getDecoder().decode(b64);
        return Keys.hmacShaKeyFor(bytes);
    }
}
