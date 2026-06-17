package com.middleware.platform.iam.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.iam.domain.TenantEncryptionKey;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Decrypts the client-encrypted PII envelope (JWE, RSA-OAEP-256 + A256GCM).
 * The {@code kid} in the JWE header selects the tenant's private key; the key
 * is verified to belong to the calling tenant before decryption. The decrypted
 * payload exists only in memory for the connector call.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PiiCryptoService {

    private final TenantKeyService keyService;
    private final ObjectMapper objectMapper;

    /**
     * @param jweCompact the JWE compact serialization from {@code encryptedPayload}
     * @param tenantId   the authenticated tenant (the key must belong to it)
     * @return the decrypted PII as a map (e.g. nationalNumber, biometrics{...})
     */
    public Map<String, Object> decrypt(String jweCompact, UUID tenantId) {
        try {
            JWEObject jwe = JWEObject.parse(jweCompact);
            String kid = jwe.getHeader().getKeyID();
            if (kid == null || kid.isBlank()) {
                throw new ApplicationException(ErrorCode.BAD_REQUEST, "Encrypted payload is missing its key id (kid)");
            }
            TenantEncryptionKey key = keyService.getByKid(kid);
            if (!key.getTenantId().equals(tenantId)) {
                throw new ApplicationException(ErrorCode.FORBIDDEN, "Encryption key does not belong to this tenant");
            }
            jwe.decrypt(new RSADecrypter(keyService.privateKey(key)));
            return objectMapper.readValue(jwe.getPayload().toString(),
                    new TypeReference<Map<String, Object>>() {});
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            // Never log the ciphertext or any decrypted content.
            log.warn("PII payload decryption failed: {}", e.getMessage());
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Could not decrypt the payload");
        }
    }
}
