-- Add GST amount to vendor purchase order items for audit-safe storage
ALTER TABLE vendor_purchase_order_item
ADD COLUMN gst_amount NUMERIC(12,2);

-- Comment
COMMENT ON COLUMN vendor_purchase_order_item.gst_amount IS 'Calculated GST amount for the item (lineTotal × gstPercentage / 100), stored as snapshot for audit safety';
