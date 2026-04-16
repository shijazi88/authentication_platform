-- ============================================================================
-- V6: Seed data — Yemen ID service, verify operation, field dictionary,
--     two reference plans (BASIC = hit-only, PREMIUM = full data).
-- ============================================================================

-- Service + operation
insert into service_definitions (id, code, name, description, connector_key, active, created_at) values
    ('11111111-1111-1111-1111-111111111111', 'YEMEN_ID', 'Yemen National ID',
     'Yemen Ministry of Interior identity verification service', 'YEMEN_ID', true, now(6));

insert into service_operations (id, service_id, code, name, description,
                                default_unit_price, currency, active, created_at) values
    ('22222222-2222-2222-2222-222222222222',
     '11111111-1111-1111-1111-111111111111',
     'verify', 'Identity Verification',
     'Verify a national number, optionally with biometric fingerprint',
     50, 'USD', true, now(6));

-- Field dictionary — every path the gateway can return for YEMEN_ID/verify.
-- Paths must match the canonical response shape produced by YemenIdConnector.toCanonical.
insert into field_definitions (id, operation_id, path, data_class, description) values
    -- transaction
    ('30000000-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222',
     'transaction.id', 'string', 'Provider transaction id'),
    ('30000000-0000-0000-0000-000000000002', '22222222-2222-2222-2222-222222222222',
     'transaction.timestamp', 'string', 'Provider transaction timestamp'),

    -- verification (hit-only payload lives here)
    ('30000000-0000-0000-0000-000000000003', '22222222-2222-2222-2222-222222222222',
     'verification.status', 'string', 'MATCH | NO_MATCH | NO_VERIFICATION_POSSIBLE'),
    ('30000000-0000-0000-0000-000000000004', '22222222-2222-2222-2222-222222222222',
     'verification.biometric.status', 'boolean', 'Biometric match flag'),
    ('30000000-0000-0000-0000-000000000005', '22222222-2222-2222-2222-222222222222',
     'verification.biometric.score', 'number', 'Biometric confidence 1-100'),

    -- person (data payload)
    ('30000000-0000-0000-0000-000000000010', '22222222-2222-2222-2222-222222222222',
     'person.nationalNumber', 'string', 'National number echo'),
    ('30000000-0000-0000-0000-000000000011', '22222222-2222-2222-2222-222222222222',
     'person.demographics', 'object', 'Full demographic block (subtree)'),
    ('30000000-0000-0000-0000-000000000012', '22222222-2222-2222-2222-222222222222',
     'person.cards', 'array', 'Issued cards (subtree)');

-- Reference plans
insert into plans (id, code, name, description, base_fee_minor, currency, active, created_at) values
    ('40000000-0000-0000-0000-000000000001', 'YEMEN_ID_BASIC',
     'Yemen ID — Hit Only',
     'Returns only the verification result and biometric confidence — no demographic data.',
     0, 'USD', true, now(6)),
    ('40000000-0000-0000-0000-000000000002', 'YEMEN_ID_PREMIUM',
     'Yemen ID — Full Data',
     'Returns full demographics, cards, and verification data.',
     0, 'USD', true, now(6));

-- Operation entitlements: both plans get access to YEMEN_ID/verify
insert into plan_entitlements (id, plan_id, operation_id, unit_price_override,
                               monthly_quota, rate_limit_per_minute) values
    ('50000000-0000-0000-0000-000000000001',
     '40000000-0000-0000-0000-000000000001',
     '22222222-2222-2222-2222-222222222222',
     30, 10000, 60),
    ('50000000-0000-0000-0000-000000000002',
     '40000000-0000-0000-0000-000000000002',
     '22222222-2222-2222-2222-222222222222',
     150, 50000, 600);

-- Field entitlements:
--   BASIC plan = transaction + verification only (hit-only)
--   PREMIUM plan = everything
insert into plan_field_entitlements (id, plan_id, field_id, field_path) values
    -- BASIC
    ('60000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001',
     '30000000-0000-0000-0000-000000000001', 'transaction.id'),
    ('60000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000001',
     '30000000-0000-0000-0000-000000000002', 'transaction.timestamp'),
    ('60000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000001',
     '30000000-0000-0000-0000-000000000003', 'verification.status'),
    ('60000000-0000-0000-0000-000000000004', '40000000-0000-0000-0000-000000000001',
     '30000000-0000-0000-0000-000000000004', 'verification.biometric.status'),
    ('60000000-0000-0000-0000-000000000005', '40000000-0000-0000-0000-000000000001',
     '30000000-0000-0000-0000-000000000005', 'verification.biometric.score'),

    -- PREMIUM (everything)
    ('60000000-0000-0000-0000-000000000010', '40000000-0000-0000-0000-000000000002',
     '30000000-0000-0000-0000-000000000001', 'transaction.id'),
    ('60000000-0000-0000-0000-000000000011', '40000000-0000-0000-0000-000000000002',
     '30000000-0000-0000-0000-000000000002', 'transaction.timestamp'),
    ('60000000-0000-0000-0000-000000000012', '40000000-0000-0000-0000-000000000002',
     '30000000-0000-0000-0000-000000000003', 'verification.status'),
    ('60000000-0000-0000-0000-000000000013', '40000000-0000-0000-0000-000000000002',
     '30000000-0000-0000-0000-000000000004', 'verification.biometric.status'),
    ('60000000-0000-0000-0000-000000000014', '40000000-0000-0000-0000-000000000002',
     '30000000-0000-0000-0000-000000000005', 'verification.biometric.score'),
    ('60000000-0000-0000-0000-000000000015', '40000000-0000-0000-0000-000000000002',
     '30000000-0000-0000-0000-000000000010', 'person.nationalNumber'),
    ('60000000-0000-0000-0000-000000000016', '40000000-0000-0000-0000-000000000002',
     '30000000-0000-0000-0000-000000000011', 'person.demographics'),
    ('60000000-0000-0000-0000-000000000017', '40000000-0000-0000-0000-000000000002',
     '30000000-0000-0000-0000-000000000012', 'person.cards');
