-- Add PO/Order No and Order Note columns to orders table
ALTER TABLE orders ADD COLUMN po_order_no VARCHAR(50);
ALTER TABLE orders ADD COLUMN order_note TEXT;

