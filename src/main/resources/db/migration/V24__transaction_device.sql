-- =============================================================================
-- V24 — Capture device on each verification (ICD v2.2 §4.2.2 `deviceId`).
--
-- device_id:         serial number of the fingerprint scanner the bank sent.
-- device_registered: whether that serial is registered for the tenant
--                    (fingerprint_devices) at the time of the call. Recorded
--                    for every call; enforced only when the admin device policy
--                    is REGISTERED_ONLY (platform_settings key DEVICE_POLICY).
-- =============================================================================

alter table transactions
    add column device_id         varchar(128) null,
    add column device_registered tinyint(1)   null;

create index idx_tx_device on transactions (tenant_id, device_id);
