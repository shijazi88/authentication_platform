-- Fingerprint-exception transactions: verifications done without a biometric
-- (person physically cannot provide a fingerprint). The fingerprint is never
-- sent to the backend; the request is recorded and flagged for lookup.
ALTER TABLE transactions
    ADD COLUMN is_exception     BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN exception_reason VARCHAR(48)  NULL,
    ADD COLUMN exception_note   VARCHAR(512) NULL;

-- Fast lookup of exception transactions (Exceptions filter + reports).
CREATE INDEX idx_transactions_exception ON transactions (is_exception, created_at);
