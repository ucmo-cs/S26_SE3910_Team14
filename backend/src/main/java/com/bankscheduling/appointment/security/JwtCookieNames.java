package com.bankscheduling.appointment.security;

/**
 * HttpOnly cookie names for token transport (never mirrored in JSON bodies).
 */
public final class JwtCookieNames {

    public static final String ACCESS_TOKEN = "AS_ACCESS";
    public static final String REFRESH_TOKEN = "AS_REFRESH";

    private JwtCookieNames() {
    }
}
