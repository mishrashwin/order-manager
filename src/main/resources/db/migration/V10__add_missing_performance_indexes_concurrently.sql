-- Online-safe index creation for environments where performance indexes are still missing.
-- This migration intentionally uses CONCURRENTLY and must run outside a transaction.

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_company_order_date
ON orders(company_id, order_date DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_company_delivery_date
ON orders(company_id, delivery_date);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_clients_company_id
ON clients(company_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_vendors_company_id
ON vendors(company_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_company_id
ON users(company_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_company_role
ON users(company_id, role);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_company_status
ON orders(company_id, status);

