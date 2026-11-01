-- Add state column to companies table
ALTER TABLE companies ADD COLUMN IF NOT EXISTS state VARCHAR(100);

-- Add state column to vendors table
ALTER TABLE vendors ADD COLUMN IF NOT EXISTS state VARCHAR(100);
