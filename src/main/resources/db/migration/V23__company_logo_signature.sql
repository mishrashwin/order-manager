-- Add logo and signature binary storage to companies table
-- Logo
ALTER TABLE companies ADD COLUMN logo_data BYTEA;
ALTER TABLE companies ADD COLUMN logo_content_type VARCHAR(100);

-- Signature
ALTER TABLE companies ADD COLUMN signature_data BYTEA;
ALTER TABLE companies ADD COLUMN signature_content_type VARCHAR(100);

-- Comments
COMMENT ON COLUMN companies.logo_data IS 'Company logo image (small size)';
COMMENT ON COLUMN companies.logo_content_type IS 'MIME type of logo image (e.g., image/png, image/jpeg)';
COMMENT ON COLUMN companies.signature_data IS 'Authorized signature image';
COMMENT ON COLUMN companies.signature_content_type IS 'MIME type of signature image (e.g., image/png, image/jpeg)';
