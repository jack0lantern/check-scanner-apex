-- Phase 2 Step 2: Store check images as binary (bytea) instead of text
-- PostgreSQL: BYTEA; H2 with MODE=PostgreSQL: BYTEA maps to VARBINARY

ALTER TABLE transfers DROP COLUMN front_image_data;
ALTER TABLE transfers ADD COLUMN front_image_data BYTEA;

ALTER TABLE transfers DROP COLUMN back_image_data;
ALTER TABLE transfers ADD COLUMN back_image_data BYTEA;
