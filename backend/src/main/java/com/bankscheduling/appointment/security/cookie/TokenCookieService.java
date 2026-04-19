package com.bankscheduling.appointment.security.cookie;

import com.bankscheduling.appointment.security.JwtCookieNames;
import com.bankscheduling.appointment.security.JwtSecurityProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Binds access and refresh JWTs to HttpOnly, Secure, SameSite=Strict cookies (never JSON).
 */
@Service
public class TokenCookieService {

    private final JwtSecurityProperties jwtProperties;

    public TokenCookieService(JwtSecurityProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public void attachTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addHeader("Set-Cookie", accessCookie(accessToken).toString());
        response.addHeader("Set-Cookie", refreshCookie(refreshToken).toString());
    }

    /** Sets only the access-token cookie (used after refresh). */
    public void attachAccessTokenCookie(HttpServletResponse response, String accessToken) {
        response.addHeader("Set-Cookie", accessCookie(accessToken).toString());
    }

    public void clearTokenCookies(HttpServletResponse response) {
        response.addHeader("Set-Cookie", deleteCookie(JwtCookieNames.ACCESS_TOKEN).toString());
        response.addHeader("Set-Cookie", deleteCookie(JwtCookieNames.REFRESH_TOKEN).toString());
    }

    private ResponseCookie accessCookie(String value) {
        return baseCookie(JwtCookieNames.ACCESS_TOKEN, value, Duration.ofSeconds(jwtProperties.getAccessTokenTtlSeconds()));
    }

    private ResponseCookie refreshCookie(String value) {
        return baseCookie(JwtCookieNames.REFRESH_TOKEN, value, Duration.ofSeconds(jwtProperties.getRefreshTokenTtlSeconds()));
    }

    private ResponseCookie baseCookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie deleteCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
    }
}
