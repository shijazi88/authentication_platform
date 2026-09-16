-- NFIQ 2 quality score (0–100, NIST) measured server-side for the fingerprint
-- image, when the quality sidecar is enabled. Null when not measured.
alter table transactions add column image_nfiq2 int null;
