-- Add Terms & Conditions fields to companies table (company-level template)
ALTER TABLE companies
ADD COLUMN vendor_po_terms_conditions TEXT,
ADD COLUMN vendor_po_terms_conditions_enabled BOOLEAN DEFAULT true;

-- Add comment for documentation
COMMENT ON COLUMN companies.vendor_po_terms_conditions IS 'JSON array of terms and conditions strings for Vendor PO PDFs (company-level template)';
COMMENT ON COLUMN companies.vendor_po_terms_conditions_enabled IS 'Toggle to include/exclude terms and conditions in Vendor PO PDFs (default: true)';
