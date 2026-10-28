-- Fix column type mismatches in vendor_purchase_order table
-- Java entities use Double which maps to FLOAT(53) in PostgreSQL
-- V18 already added the missing columns, now we fix the type mismatches

-- Fix total_amount type in vendor_purchase_order table
ALTER TABLE vendor_purchase_order ALTER COLUMN total_amount TYPE FLOAT(53);

-- Fix column types in vendor_purchase_order_item table
-- First drop the generated column, then alter types, then recreate generated column
ALTER TABLE vendor_purchase_order_item DROP COLUMN IF EXISTS line_total;

ALTER TABLE vendor_purchase_order_item ALTER COLUMN quantity TYPE FLOAT(53);
ALTER TABLE vendor_purchase_order_item ALTER COLUMN unit_price TYPE FLOAT(53);

-- Recreate the generated column with correct type
ALTER TABLE vendor_purchase_order_item ADD COLUMN line_total FLOAT(53) GENERATED ALWAYS AS (quantity * unit_price) STORED;

-- Add comments for documentation
COMMENT ON COLUMN vendor_purchase_order.total_amount IS 'Total amount of the purchase order (FLOAT for Java Double compatibility)';
COMMENT ON COLUMN vendor_purchase_order_item.quantity IS 'Quantity of items (FLOAT for Java Double compatibility)';
COMMENT ON COLUMN vendor_purchase_order_item.unit_price IS 'Unit price of items (FLOAT for Java Double compatibility)';
COMMENT ON COLUMN vendor_purchase_order_item.line_total IS 'Line total calculated as quantity * unit_price (FLOAT for Java Double compatibility)';
