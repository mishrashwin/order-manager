-- Owner approval workflow for tenant onboarding
ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP NULL,
    ADD COLUMN IF NOT EXISTS approved_by VARCHAR(100) NULL;

-- Existing companies are grandfathered as approved.
UPDATE companies
SET approval_status = 'APPROVED'
WHERE approval_status IS NULL;

