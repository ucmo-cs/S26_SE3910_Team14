package com.bankscheduling.appointment.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scheduling.security.jwt")
public class JwtSecurityProperties {

    private String issuer = "appointment-scheduling";
    /** Default 15 minutes */
    private int accessTokenTtlSeconds = 900;
    /** Default 7 days */
    private int refreshTokenTtlSeconds = 604_800;
    private String signingKeyBase64 = "";

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public int getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public void setAccessTokenTtlSeconds(int accessTokenTtlSeconds) {
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public int getRefreshTokenTtlSeconds() {
        return refreshTokenTtlSeconds;
    }

    public void setRefreshTokenTtlSeconds(int refreshTokenTtlSeconds) {
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    public String getSigningKeyBase64() {
        return signingKeyBase64;
    }

    public void setSigningKeyBase64(String signingKeyBase64) {
        this.signingKeyBase64 = signingKeyBase64;
    }
}
