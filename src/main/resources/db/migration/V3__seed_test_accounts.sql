-- Phase 2 Step 3: Seed test accounts for Account Resolution Service
-- TEST001: Clean pass account for happy path and manual testing
-- Portable for PostgreSQL and H2 (tests)
INSERT INTO accounts (id, external_id, internal_number, routing_number, omnibus_id, account_type, created_at, updated_at)
SELECT 'a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d', 'TEST001', 'INT-12345678', '021000021', 'OMN-999', 'RETIREMENT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE external_id = 'TEST001');
