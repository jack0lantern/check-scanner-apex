-- Store investor's external account ID at submission time for accurate operator queue display
ALTER TABLE transfers ADD COLUMN investor_account_id VARCHAR(50);

-- Backfill from accounts lookup: to_account_id is internal_number (portable for PostgreSQL and H2)
UPDATE transfers
SET investor_account_id = (SELECT external_id FROM accounts WHERE internal_number = transfers.to_account_id LIMIT 1)
WHERE investor_account_id IS NULL;
