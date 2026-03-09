-- Migration to fix data integrity issue with client names in orders
-- Add proper foreign key relationship between orders and clients
-- Compatible with PostgreSQL

-- Step 1: Add client_id column if it doesn't exist (nullable initially to handle existing data)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'client_id'
    ) THEN
        ALTER TABLE orders ADD COLUMN client_id BIGINT;
    END IF;
END $$;

-- Step 2: Migrate existing data - match customer_name to client names
-- This will link existing orders to clients where names match
UPDATE orders o
SET client_id = (
    SELECT c.id
    FROM clients c
    WHERE UPPER(c.name) = UPPER(o.customer_name)
    AND c.company_id = o.company_id
    ORDER BY c.id
    LIMIT 1
)
WHERE o.customer_name IS NOT NULL AND o.client_id IS NULL;

-- Step 3: Add foreign key constraint (if not already exists)
DO $$
BEGIN
    -- Check if constraint already exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'orders' AND constraint_name = 'fk_orders_client'
    ) THEN
        ALTER TABLE orders
        ADD CONSTRAINT fk_orders_client
        FOREIGN KEY (client_id)
        REFERENCES clients(id)
        ON DELETE SET NULL;
    END IF;
END $$;

-- Step 4: Create index for better query performance (if not already exists)
DO $$
BEGIN
    IF to_regclass('public.idx_orders_client_id') IS NULL THEN
        CREATE INDEX idx_orders_client_id ON orders(client_id);
    END IF;
END $$;
