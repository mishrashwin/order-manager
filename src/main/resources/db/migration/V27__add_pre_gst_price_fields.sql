-- Add pre-GST price fields to support new GST calculation approach
-- Users will now input pre-GST prices and system will calculate GST-inclusive prices

-- Add pre-GST price to products table
ALTER TABLE products 
ADD COLUMN pre_gst_price DECIMAL(12,2) DEFAULT NULL,
ADD COLUMN gst_inclusive_price DECIMAL(12,2) DEFAULT NULL;

-- Add pre-GST price to order_items table  
ALTER TABLE order_items
ADD COLUMN pre_gst_unit_price DECIMAL(12,2) DEFAULT NULL,
ADD COLUMN gst_inclusive_unit_price DECIMAL(12,2) DEFAULT NULL,
ADD COLUMN gst_amount DECIMAL(12,2) DEFAULT NULL;

-- Add pre-GST price to vendor_purchase_order_item table
ALTER TABLE vendor_purchase_order_item
ADD COLUMN pre_gst_unit_price DECIMAL(12,2) DEFAULT NULL,
ADD COLUMN gst_inclusive_unit_price DECIMAL(12,2) DEFAULT NULL;

-- Migrate existing data: assume current prices are GST-inclusive and calculate pre-GST prices
-- For products
UPDATE products 
SET pre_gst_price = CASE 
    WHEN gst_percentage IS NOT NULL AND gst_percentage > 0 
    THEN CAST(price / (1 + gst_percentage/100) AS DECIMAL(12,2))
    ELSE CAST(price AS DECIMAL(12,2))
END,
gst_inclusive_price = CAST(price AS DECIMAL(12,2))
WHERE price IS NOT NULL;

-- For order items, back-calculate from the product's GST rate when available
UPDATE order_items oi
SET pre_gst_unit_price = CASE
    WHEN oi.unit_price IS NULL THEN NULL
    WHEN oi.product_id IS NOT NULL AND p.gst_percentage IS NOT NULL AND p.gst_percentage > 0
      THEN CAST(oi.unit_price / (1 + p.gst_percentage / 100.0) AS DECIMAL(12,2))
    ELSE CAST(oi.unit_price AS DECIMAL(12,2))
END,
gst_inclusive_unit_price = CAST(oi.unit_price AS DECIMAL(12,2)),
gst_amount = CASE
    WHEN oi.unit_price IS NULL THEN NULL
    WHEN oi.product_id IS NOT NULL AND p.gst_percentage IS NOT NULL AND p.gst_percentage > 0
      THEN CAST(oi.unit_price - (oi.unit_price / (1 + p.gst_percentage / 100.0)) AS DECIMAL(12,2))
    ELSE CAST(0 AS DECIMAL(12,2))
END
FROM products p
WHERE oi.product_id = p.id
  AND oi.unit_price IS NOT NULL;

UPDATE order_items oi
SET pre_gst_unit_price = CAST(oi.unit_price AS DECIMAL(12,2)),
    gst_inclusive_unit_price = CAST(oi.unit_price AS DECIMAL(12,2)),
    gst_amount = CAST(0 AS DECIMAL(12,2))
WHERE oi.unit_price IS NOT NULL
  AND oi.product_id IS NULL;

-- For vendor PO items
UPDATE vendor_purchase_order_item
SET pre_gst_unit_price = CASE 
    WHEN unit_price IS NOT NULL AND gst_percentage IS NOT NULL AND gst_percentage > 0
    THEN CAST(unit_price / (1 + gst_percentage/100) AS DECIMAL(12,2))
    ELSE CAST(unit_price AS DECIMAL(12,2))
END,
gst_inclusive_unit_price = CAST(unit_price AS DECIMAL(12,2))
WHERE unit_price IS NOT NULL;

-- Add comments to document the new fields
COMMENT ON COLUMN products.pre_gst_price IS 'Pre-GST unit price entered by user';
COMMENT ON COLUMN products.gst_inclusive_price IS 'GST-inclusive unit price calculated by system';
COMMENT ON COLUMN order_items.pre_gst_unit_price IS 'Pre-GST unit price at time of order';
COMMENT ON COLUMN order_items.gst_inclusive_unit_price IS 'GST-inclusive unit price at time of order';
COMMENT ON COLUMN order_items.gst_amount IS 'GST amount per unit';
COMMENT ON COLUMN vendor_purchase_order_item.pre_gst_unit_price IS 'Pre-GST unit price for PO item';
COMMENT ON COLUMN vendor_purchase_order_item.gst_inclusive_unit_price IS 'GST-inclusive unit price for PO item';
