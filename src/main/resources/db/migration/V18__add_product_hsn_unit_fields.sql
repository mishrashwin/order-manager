-- Add HSN code and unit fields to products table for Vendor PO functionality
ALTER TABLE products ADD COLUMN hsn_code VARCHAR(50);
ALTER TABLE products ADD COLUMN unit VARCHAR(20);

-- Add GSTN field to vendors table for GST compliance and Vendor PO generation
ALTER TABLE vendors ADD COLUMN gstn VARCHAR(50);

-- Add indexes for performance if these fields will be queried frequently
CREATE INDEX IF NOT EXISTS idx_products_hsn_code ON products(hsn_code);
CREATE INDEX IF NOT EXISTS idx_products_unit ON products(unit);
CREATE INDEX IF NOT EXISTS idx_vendors_gstn ON vendors(gstn);

-- Add comments for documentation
COMMENT ON COLUMN products.hsn_code IS 'HSN/SAC code for GST compliance and Vendor PO generation';
COMMENT ON COLUMN products.unit IS 'Unit of measurement (PCS, KG, BOX, etc.) for Vendor PO generation';
COMMENT ON COLUMN vendors.gstn IS 'GSTIN number for GST compliance and Vendor PO generation';
