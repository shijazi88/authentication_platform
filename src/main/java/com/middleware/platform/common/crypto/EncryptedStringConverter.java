package com.middleware.platform.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA attribute converter for at-rest encryption of string columns using AES-GCM.
 *
 * <p>Ciphertext layout (Base64-encoded): {@code ENC1:<base64(iv||ciphertext||tag)>}.
 * A fresh 12-byte IV is generated for every write. Values written WITHOUT the
 * {@code ENC1:} prefix are treated as plaintext (legacy / seed rows) and are
 * transparently re-encrypted on the next write — this lets Flyway seeds hold
 * plaintext that upgrades cleanly once the key is configured.
 *
 * <p>Key source: {@code platform.security.column-encryption-key} (base64 of a
 * 32-byte AES-256 key, or any string that will be hashed to 32 bytes). If the
 * property is absent or blank, the converter passes through plaintext and
 * logs a WARN — useful in dev/CI; NEVER run prod without a key.
 */
@Slf4j
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String PREFIX = "ENC1:";
    private static final String ALGO = "AES";
    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int IV_LEN = 12;
    private static final int TAG_LEN_BITS = 128;

    private static volatile SecretKeySpec key;
    private static volatile boolean warned;

    public EncryptedStringConverter() { /* Spring no-arg ctor */ }

    @Value("${platform.security.column-encryption-key:}")
    public void setKeyMaterial(String keyMaterial) {
        if (keyMaterial == null || keyMaterial.isBlank()) {
            key = null;
            return;
        }
        try {
            // Accept either a plain passphrase or a base64 32-byte key.
            // Passphrase is hashed with SHA-256 to derive a stable 32-byte key.
            byte[] bytes;
            try {
                byte[] decoded = Base64.getDecoder().decode(keyMaterial);
                if (decoded.length == 32) {
                    bytes = decoded;
                } else {
                    bytes = sha256(keyMaterial);
                }
            } catch (IllegalArgumentException notBase64) {
                bytes = sha256(keyMaterial);
            }
            key = new SecretKeySpec(bytes, ALGO);
            log.info("Column encryption configured · AES-256-GCM");
        } catch (Exception ex) {
            log.error("Failed to configure column encryption key — falling back to plaintext", ex);
            key = null;
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        SecretKeySpec k = key;
        if (k == null) {
            warnOnce();
            return attribute;
        }
        try {
            byte[] iv = new byte[IV_LEN];
            new SecureRandom().nextBytes(iv);
            Cipher c = Cipher.getInstance(CIPHER);
            c.init(Cipher.ENCRYPT_MODE, k, new GCMParameterSpec(TAG_LEN_BITS, iv));
            byte[] ct = c.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (Exception ex) {
            throw new IllegalStateException("Column encryption failed", ex);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        if (!dbData.startsWith(PREFIX)) {
            // Legacy / seed value stored as plaintext — return as-is.
            return dbData;
        }
        SecretKeySpec k = key;
        if (k == null) {
            throw new IllegalStateException(
                    "Encountered an encrypted column value but no encryption key is configured " +
                    "(set platform.security.column-encryption-key).");
        }
        try {
            byte[] raw = Base64.getDecoder().decode(dbData.substring(PREFIX.length()));
            byte[] iv = new byte[IV_LEN];
            System.arraycopy(raw, 0, iv, 0, IV_LEN);
            byte[] ct = new byte[raw.length - IV_LEN];
            System.arraycopy(raw, IV_LEN, ct, 0, ct.length);
            Cipher c = Cipher.getInstance(CIPHER);
            c.init(Cipher.DECRYPT_MODE, k, new GCMParameterSpec(TAG_LEN_BITS, iv));
            byte[] pt = c.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Column decryption failed", ex);
        }
    }

    private static byte[] sha256(String s) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
    }

    private static void warnOnce() {
        if (!warned) {
            warned = true;
            log.warn("platform.security.column-encryption-key is not set — storing sensitive columns in PLAINTEXT. Set a key in production.");
        }
    }
}
