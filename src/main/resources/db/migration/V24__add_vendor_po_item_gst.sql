-- Add GST percentage to vendor purchase order items
ALTER TABLE vendor_purchase_order_item
ADD COLUMN gst_percentage NUMERIC(5,2);

-- Comment
COMMENT ON COLUMN vendor_purchase_order_item.gst_percentage IS 'GST percentage applicable for the item (snapshot from product at PO creation)';
