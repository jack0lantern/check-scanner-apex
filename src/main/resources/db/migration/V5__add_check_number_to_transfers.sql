-- Add parsed MICR fields to transfers for check-number duplicate detection
-- Portable for PostgreSQL and H2 (tests)
ALTER TABLE transfers ADD COLUMN micr_routing_number VARCHAR(20);
ALTER TABLE transfers ADD COLUMN micr_account_number VARCHAR(50);
ALTER TABLE transfers ADD COLUMN micr_check_number VARCHAR(10);
