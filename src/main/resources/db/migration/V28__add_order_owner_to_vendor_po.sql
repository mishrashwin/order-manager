-- Add order_owner field to vendor_purchase_order table
-- This field tracks the employee responsible for bringing the order

ALTER TABLE vendor_purchase_order
ADD COLUMN order_owner_id BIGINT NULL;

-- Add foreign key constraint to users table
ALTER TABLE vendor_purchase_order
ADD CONSTRAINT fk_vendor_po_order_owner
FOREIGN KEY (order_owner_id) REFERENCES users(id) ON DELETE SET NULL;

-- Add comment to document the new field
COMMENT ON COLUMN vendor_purchase_order.order_owner_id IS 'Employee responsible for bringing the order (optional)';
