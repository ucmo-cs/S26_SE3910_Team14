package com.bankscheduling.appointment.entity.converter;

import com.bankscheduling.appointment.config.properties.PiiCryptoProperties;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Application-layer AES-256-GCM converter for customer PII columns.
 * Stored form: base64(iv || ciphertext||tag).
 */
@Component
@Converter(autoApply = false)
public class PiiEncryptionConverter implements AttributeConverter<String, String> {

    private static final String AES = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final PiiCryptoProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public PiiEncryptionConverter(PiiCryptoProperties properties) {
        this.properties = properties;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        byte[] keyBytes = decodeKey();
        SecretKey key = new SecretKeySpec(keyBytes, AES);
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("PII encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        byte[] keyBytes = decodeKey();
        SecretKey key = new SecretKeySpec(keyBytes, AES);
        try {
            byte[] decoded = Base64.getDecoder().decode(dbData);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] cipherBytes = new byte[buffer.remaining()];
            buffer.get(cipherBytes);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plain = cipher.doFinal(cipherBytes);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("PII decryption failed", e);
        }
    }

    private byte[] decodeKey() {
        String b64 = properties.getAesKeyBase64();
        if (b64 == null || b64.isBlank()) {
            throw new IllegalStateException("PII AES key missing: set scheduling.pii.aes-key-base64 (256-bit key, base64)");
        }
        byte[] keyBytes = Base64.getDecoder().decode(b64);
        if (keyBytes.length != 32) {
            throw new IllegalStateException("PII AES key must decode to 32 bytes (AES-256)");
        }
        return keyBytes;
    }
}
