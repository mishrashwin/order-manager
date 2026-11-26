-- Add contact person details to company addresses for PO email CC functionality
ALTER TABLE company_addresses
ADD COLUMN contact_person_name VARCHAR(120),
ADD COLUMN contact_number VARCHAR(20),
ADD COLUMN contact_email VARCHAR(255);

-- Add comments for documentation
COMMENT ON COLUMN company_addresses.contact_person_name IS 'Contact person name at this address location';
COMMENT ON COLUMN company_addresses.contact_number IS 'Contact phone number at this address location';
COMMENT ON COLUMN company_addresses.contact_email IS 'Contact email at this address location - receives PO email CC';
