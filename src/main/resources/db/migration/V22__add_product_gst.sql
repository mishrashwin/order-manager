-- Add GST percentage to products
ALTER TABLE products
ADD COLUMN gst_percentage NUMERIC(5,2);

-- Comment
COMMENT ON COLUMN products.gst_percentage IS 'GST percentage applicable for the product (e.g., 5, 12, 18)';
