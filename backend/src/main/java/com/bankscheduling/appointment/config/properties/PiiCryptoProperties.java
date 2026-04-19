package com.bankscheduling.appointment.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scheduling.pii")
public class PiiCryptoProperties {

    /**
     * Base64-encoded 256-bit AES key material for field-level encryption.
     */
    private String aesKeyBase64 = "";

    public String getAesKeyBase64() {
        return aesKeyBase64;
    }

    public void setAesKeyBase64(String aesKeyBase64) {
        this.aesKeyBase64 = aesKeyBase64;
    }
}
