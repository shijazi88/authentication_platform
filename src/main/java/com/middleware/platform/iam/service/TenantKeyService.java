package com.middleware.platform.iam.service;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.common.util.Ids;
import com.middleware.platform.iam.domain.EncryptionKeyStatus;
import com.middleware.platform.iam.domain.Tenant;
import com.middleware.platform.iam.domain.TenantEncryptionKey;
import com.middleware.platform.iam.repo.TenantEncryptionKeyRepository;
import com.middleware.platform.iam.repo.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Manages per-tenant RSA encryption keypairs used for PII payload encryption.
 * Generates a self-signed X.509 certificate per key, stores the private key
 * encrypted at rest, and supports rotation. Every tenant is guaranteed an
 * ACTIVE key (backfilled at startup, created lazily otherwise).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantKeyService {

    private static final int RSA_BITS = 3072;
    private static final String ALGORITHM = "RSA-OAEP-256";
    private static final long VALIDITY_DAYS = 730;

    private final TenantEncryptionKeyRepository keys;
    private final TenantRepository tenants;

    /** Ensure every existing tenant has an ACTIVE encryption key. */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillKeys() {
        for (Tenant t : tenants.findAll()) {
            if (keys.findFirstByTenantIdAndStatusOrderByCreatedAtDesc(t.getId(), EncryptionKeyStatus.ACTIVE).isEmpty()) {
                generate(t);
                log.info("Backfilled encryption key for tenant {}", t.getId());
            }
        }
    }

    @Transactional
    public TenantEncryptionKey getOrCreateActive(UUID tenantId) {
        return keys.findFirstByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, EncryptionKeyStatus.ACTIVE)
                .orElseGet(() -> {
                    Tenant t = tenants.findById(tenantId)
                            .orElseThrow(() -> ApplicationException.notFound("Tenant"));
                    return generate(t);
                });
    }

    @Transactional(readOnly = true)
    public java.util.List<TenantEncryptionKey> listKeys(UUID tenantId) {
        return keys.findByTenantId(tenantId);
    }

    /** Revoke a non-active key (e.g. a RETIRING one) by kid. */
    @Transactional
    public void revoke(UUID tenantId, String kid) {
        TenantEncryptionKey key = keys.findByKid(kid)
                .orElseThrow(() -> ApplicationException.notFound("Encryption key"));
        if (!key.getTenantId().equals(tenantId)) {
            throw ApplicationException.notFound("Encryption key");
        }
        if (key.getStatus() == EncryptionKeyStatus.ACTIVE) {
            throw new ApplicationException(ErrorCode.CONFLICT,
                    "Cannot revoke the active key — rotate first, then revoke the retiring one");
        }
        key.setStatus(EncryptionKeyStatus.REVOKED);
    }

    @Transactional(readOnly = true)
    public TenantEncryptionKey getByKid(String kid) {
        return keys.findByKid(kid)
                .orElseThrow(() -> new ApplicationException(ErrorCode.BAD_REQUEST, "Unknown encryption key id"));
    }

    /** Rotate: current ACTIVE → RETIRING, then mint a fresh ACTIVE key. */
    @Transactional
    public TenantEncryptionKey rotate(UUID tenantId) {
        keys.findFirstByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, EncryptionKeyStatus.ACTIVE)
                .ifPresent(k -> {
                    k.setStatus(EncryptionKeyStatus.RETIRING);
                    k.setRotatedAt(Instant.now());
                });
        Tenant t = tenants.findById(tenantId).orElseThrow(() -> ApplicationException.notFound("Tenant"));
        return generate(t);
    }

    private TenantEncryptionKey generate(Tenant tenant) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(RSA_BITS);
            KeyPair kp = kpg.generateKeyPair();

            Instant now = Instant.now();
            Instant exp = now.plus(VALIDITY_DAYS, ChronoUnit.DAYS);
            X509Certificate cert = selfSign(kp, tenant.getCode(), now, exp);

            TenantEncryptionKey key = TenantEncryptionKey.builder()
                    .tenantId(tenant.getId())
                    .kid("key_" + Ids.newClientId().substring(4))
                    .algorithm(ALGORITHM)
                    .publicCertPem(pem("CERTIFICATE", cert.getEncoded()))
                    .privateKeyPem(pem("PRIVATE KEY", kp.getPrivate().getEncoded()))
                    .status(EncryptionKeyStatus.ACTIVE)
                    .createdAt(now)
                    .expiresAt(exp)
                    .build();
            return keys.save(key);
        } catch (Exception e) {
            throw new ApplicationException(ErrorCode.INTERNAL_ERROR, "Failed to generate encryption key", e);
        }
    }

    private static X509Certificate selfSign(KeyPair kp, String tenantCode, Instant from, Instant to) throws Exception {
        X500Principal dn = new X500Principal("CN=" + safeCn(tenantCode) + ", O=MOTABIQ");
        BigInteger serial = new BigInteger(64, new SecureRandom());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate());
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                dn, serial, Date.from(from), Date.from(to), dn, kp.getPublic());
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }

    /** RSA private key for decryption — usable for ACTIVE and RETIRING keys. */
    public RSAPrivateKey privateKey(TenantEncryptionKey key) {
        if (key.getStatus() == EncryptionKeyStatus.REVOKED) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Encryption key is revoked");
        }
        try {
            String base64 = key.getPrivateKeyPem()
                    .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                    .replaceAll("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(base64);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new ApplicationException(ErrorCode.INTERNAL_ERROR, "Failed to load encryption key", e);
        }
    }

    /** SHA-256 fingerprint of the certificate (hex, colon-separated). */
    public String fingerprint(TenantEncryptionKey key) {
        try {
            String base64 = key.getPublicCertPem()
                    .replaceAll("-----BEGIN CERTIFICATE-----", "")
                    .replaceAll("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(base64);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(der);
            String hex = HexFormat.of().withUpperCase().formatHex(digest);
            return String.join(":", hex.split("(?<=\\G..)"));
        } catch (Exception e) {
            return "";
        }
    }

    private static String pem(String type, byte[] der) {
        String b64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + b64 + "\n-----END " + type + "-----\n";
    }

    private static String safeCn(String code) {
        if (code == null || code.isBlank()) return "tenant";
        return code.replaceAll("[^A-Za-z0-9_.-]", "_");
    }
}
