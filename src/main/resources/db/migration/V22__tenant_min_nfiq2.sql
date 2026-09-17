-- =============================================================================
-- V22 — Per-bank minimum NFIQ 2 fingerprint quality score.
--
-- min_nfiq2: NULL → use the platform default (Settings → Fingerprint image);
--            0–100 → this bank's own threshold (different scanners score
--            differently, so one global number does not fit every client).
-- =============================================================================

alter table tenants
    add column min_nfiq2 int null;
