-- Add company branding fields for PDF generation
-- Add company address
ALTER TABLE companies
ADD COLUMN address TEXT;

-- Add logo URL
ALTER TABLE companies
ADD COLUMN logo_url VARCHAR(500);

-- Add signature URL
ALTER TABLE companies
ADD COLUMN signature_url VARCHAR(500);

-- Comments
COMMENT ON COLUMN companies.address IS 'Full company address for invoices and POs';
COMMENT ON COLUMN companies.logo_url IS 'Company logo URL for branding in PDF';
COMMENT ON COLUMN companies.signature_url IS 'Authorized signature image URL for documents';
