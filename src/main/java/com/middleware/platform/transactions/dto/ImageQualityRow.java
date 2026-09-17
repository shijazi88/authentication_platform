package com.middleware.platform.transactions.dto;

import java.util.UUID;

/**
 * Fingerprint-quality figures for one bank over a date range (admin report).
 *
 * @param images        requests that carried a fingerprint image and were inspected
 * @param scored        images that received an NFIQ 2 score
 * @param avgNfiq2      mean NFIQ 2 score of the scored images (null if none)
 * @param minNfiq2      lowest score seen
 * @param rejected      images the platform rejected (400 / 1002)
 * @param warned        images flagged in warn-only mode
 * @param rejectRate    rejected ÷ images, 0.0–1.0
 * @param buckets       scored images per NFIQ 2 band: [0–19, 20–39, 40–59, 60–79, 80–100]
 */
public record ImageQualityRow(
        UUID tenantId,
        String tenantName,
        long images,
        long scored,
        Double avgNfiq2,
        Integer minNfiq2,
        long rejected,
        long warned,
        double rejectRate,
        long[] buckets
) {}
