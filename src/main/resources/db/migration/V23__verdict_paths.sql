-- =============================================================================
-- V23 — Align the verification field paths with the ICD (§4.3.2 / §5).
--
-- The field dictionary and plan entitlements were seeded from the WireMock stub
-- shape (verification.status / verification.biometric.*). The real provider and
-- the ICD use verification.verification / verification.biometrics.*, so the
-- projector dropped the whole verdict block from every bank response.
-- Connectors now normalise the provider body into the ICD shape; the paths
-- below make the entitlements match it.
--
-- transactions.verdict: MATCH | NO_MATCH | NO_VERIFICATION_POSSIBLE | EXEMPT,
-- recorded on every processed (200) verification so operators can tell a
-- no-match from a match without opening the payload.
-- =============================================================================

update field_definitions
   set path = 'verification.verification',
       description = 'Verdict: MATCH | NO_MATCH | NO_VERIFICATION_POSSIBLE | EXEMPT'
 where path = 'verification.status';

update field_definitions
   set path = 'verification.biometrics.exists',
       description = 'Whether a biometric comparison was performed'
 where path = 'verification.biometric.status';

update field_definitions
   set path = 'verification.biometrics.score',
       description = 'Biometric match score (provider scale, 0-100)'
 where path = 'verification.biometric.score';

update plan_field_entitlements set field_path = 'verification.verification'      where field_path = 'verification.status';
update plan_field_entitlements set field_path = 'verification.biometrics.exists' where field_path = 'verification.biometric.status';
update plan_field_entitlements set field_path = 'verification.biometrics.score'  where field_path = 'verification.biometric.score';

alter table transactions
    add column verdict varchar(32) null;
