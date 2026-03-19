-- Fix approval_status default from APPROVED to PENDING
-- This handles environments where V6 was applied with the old default value (APPROVED)
-- Production already has PENDING, so this is idempotent (no-op for prod)

-- For databases where approval_status still has DEFAULT 'APPROVED', update it to 'PENDING'
-- This is safe because:
-- 1. Production already has DEFAULT 'PENDING' so won't be affected
-- 2. Local/test databases will be corrected
-- 3. Data values are also updated to match the new default

ALTER TABLE companies
    ALTER COLUMN approval_status SET DEFAULT 'PENDING';

-- Update any existing companies that still have APPROVED status (from initial V6 migration)
-- to PENDING to be consistent. Production companies already updated in V6, so this is a no-op there.
UPDATE companies
SET approval_status = 'PENDING'
WHERE approval_status = 'APPROVED' AND approved_at IS NULL;

