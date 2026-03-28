-- Remove legacy orders.customer_name persistence now that order client names are read from orders.client_id.
-- Backfill client_id from the legacy customer_name where possible before dropping the column.

UPDATE orders o
SET client_id = (
    SELECT c.id
    FROM clients c
    WHERE UPPER(c.name) = UPPER(o.customer_name)
      AND c.company_id = o.company_id
    ORDER BY c.id
    LIMIT 1
)
WHERE o.client_id IS NULL
  AND o.customer_name IS NOT NULL;

ALTER TABLE orders
DROP COLUMN IF EXISTS customer_name;

