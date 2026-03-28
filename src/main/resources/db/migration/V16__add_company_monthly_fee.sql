-- V16: Add monthly_fee column to companies table
-- Allows the owner to set a subscription fee amount per company (tenant)
ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS monthly_fee NUMERIC(10, 2);
