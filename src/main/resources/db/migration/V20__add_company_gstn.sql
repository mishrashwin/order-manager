-- Add GSTN field to companies table for PO generation
ALTER TABLE companies ADD COLUMN gstn VARCHAR(50);

-- Add index for performance if GSTN will be queried frequently
CREATE INDEX IF NOT EXISTS idx_companies_gstn ON companies(gstn);

-- Add comment for documentation
COMMENT ON COLUMN companies.gstn IS 'Company GSTIN for tax compliance and PO generation';
