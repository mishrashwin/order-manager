-- Add contact person details to Vendor PO address snapshots for Bill To and Ship To
ALTER TABLE vendor_purchase_order
ADD COLUMN bill_to_contact_person_name VARCHAR(120),
ADD COLUMN bill_to_contact_number VARCHAR(20),
ADD COLUMN bill_to_contact_email VARCHAR(255),
ADD COLUMN ship_to_contact_person_name VARCHAR(120),
ADD COLUMN ship_to_contact_number VARCHAR(20),
ADD COLUMN ship_to_contact_email VARCHAR(255);

-- Add comments for documentation
COMMENT ON COLUMN vendor_purchase_order.bill_to_contact_person_name IS 'Snapshot of Bill To address contact person name';
COMMENT ON COLUMN vendor_purchase_order.bill_to_contact_number IS 'Snapshot of Bill To address contact phone number';
COMMENT ON COLUMN vendor_purchase_order.bill_to_contact_email IS 'Snapshot of Bill To address contact email - receives PO email CC';
COMMENT ON COLUMN vendor_purchase_order.ship_to_contact_person_name IS 'Snapshot of Ship To address contact person name';
COMMENT ON COLUMN vendor_purchase_order.ship_to_contact_number IS 'Snapshot of Ship To address contact phone number';
COMMENT ON COLUMN vendor_purchase_order.ship_to_contact_email IS 'Snapshot of Ship To address contact email - receives PO email CC';
