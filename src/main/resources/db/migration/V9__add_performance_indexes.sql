-- Migration to add performance indexes for tenant-filtered queries
-- These indexes optimize the most frequently accessed query patterns in production

-- Index for orders filtered by company_id and order date (list/dashboard queries)
CREATE INDEX IF NOT EXISTS idx_orders_company_order_date
ON orders(company_id, order_date DESC);

-- Index for orders filtered by company_id and delivery date (urgent notifications)
CREATE INDEX IF NOT EXISTS idx_orders_company_delivery_date
ON orders(company_id, delivery_date);

-- Index for clients by company_id (list views)
CREATE INDEX IF NOT EXISTS idx_clients_company_id
ON clients(company_id);

-- Index for vendors by company_id (list views)
CREATE INDEX IF NOT EXISTS idx_vendors_company_id
ON vendors(company_id);

-- Index for users by company_id (admin panel)
CREATE INDEX IF NOT EXISTS idx_users_company_id
ON users(company_id);

-- Index for users by company_id and role (admin count queries)
CREATE INDEX IF NOT EXISTS idx_users_company_role
ON users(company_id, role);

-- Index for order status lookups (dashboard, list views)
CREATE INDEX IF NOT EXISTS idx_orders_company_status
ON orders(company_id, status);


