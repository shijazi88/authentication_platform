-- ============================================================================
-- V7: Switch catalog currencies to Yemeni Rial (YER) with realistic pricing.
--
-- The original V6 seed used USD with token cents-level prices (50 ¢ per call).
-- Production rates for Yemen ID verification are denominated in YER. This
-- migration:
--   1. Converts service operations + plans + plan entitlements from USD to YER.
--   2. Re-prices to commercially realistic YER amounts (BASIC 50 YER per call,
--      PREMIUM 250 YER per call). Prices remain in minor units (1 YER = 100 fils
--      per the schema convention) so the formatter logic stays consistent.
--
-- NOTE: We deliberately do NOT touch existing rows in `transactions` or
--       `billing_events`. Those are audit/financial records — they were billed
--       in USD when they happened and history should be preserved. Only the
--       catalog (forward-facing pricing) is updated. From this migration onward
--       new transactions will be priced and billed in YER.
-- ============================================================================

-- Service operations: re-price + switch currency
update service_operations
   set default_unit_price = 10000,  -- 100.00 YER (default)
       currency           = 'YER'
 where id = '22222222-2222-2222-2222-222222222222';

-- Plans: switch currency
update plans
   set currency = 'YER'
 where id in (
        '40000000-0000-0000-0000-000000000001',  -- BASIC
        '40000000-0000-0000-0000-000000000002'   -- PREMIUM
       );

-- Plan entitlements: re-price overrides
update plan_entitlements
   set unit_price_override = 5000   -- 50.00 YER per call (BASIC)
 where id = '50000000-0000-0000-0000-000000000001';

update plan_entitlements
   set unit_price_override = 25000  -- 250.00 YER per call (PREMIUM)
 where id = '50000000-0000-0000-0000-000000000002';
