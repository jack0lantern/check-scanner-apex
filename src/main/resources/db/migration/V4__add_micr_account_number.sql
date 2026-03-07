-- Add micr_account_number to accounts for MICR validation
-- Portable for PostgreSQL and H2 (tests)
ALTER TABLE accounts ADD COLUMN micr_account_number VARCHAR(50);

-- Backfill TEST001 to match stub MICR 02100002112345678901 (account 12345678)
UPDATE accounts SET micr_account_number = '12345678' WHERE external_id = 'TEST001';
