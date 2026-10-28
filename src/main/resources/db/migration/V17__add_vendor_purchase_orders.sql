-- Flyway migration: add vendor purchase orders and items
-- Version: V17
-- Note: requires existing tables `companies` and `vendors`

CREATE TABLE vendor_purchase_order (
  id BIGSERIAL PRIMARY KEY,
  company_id BIGINT NOT NULL REFERENCES companies(id),
  vendor_id BIGINT REFERENCES vendors(id),
  vendor_name VARCHAR(255) NOT NULL,
  vendor_gstn VARCHAR(64),
  po_number VARCHAR(128) NOT NULL,
  delivery_date DATE,
  linked_order_po_nos TEXT,
  emails TEXT,
  note TEXT,
  total_amount NUMERIC(14,2) DEFAULT 0 NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_by VARCHAR(128),
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);

CREATE UNIQUE INDEX ux_vendor_po_company_po_number ON vendor_purchase_order(company_id, po_number);
CREATE INDEX idx_vendor_po_company_status ON vendor_purchase_order(company_id, status);

CREATE TABLE vendor_purchase_order_item (
  id BIGSERIAL PRIMARY KEY,
  vendor_po_id BIGINT NOT NULL REFERENCES vendor_purchase_order(id) ON DELETE CASCADE,
  product_id BIGINT,
  product_name VARCHAR(255) NOT NULL,
  hsn_code VARCHAR(64),
  unit VARCHAR(64),
  quantity NUMERIC(12,3) DEFAULT 0,
  unit_price NUMERIC(14,2) DEFAULT 0,
  line_total NUMERIC(14,2) GENERATED ALWAYS AS (quantity * unit_price) STORED
);

CREATE INDEX idx_vendor_po_item_vendor_po_id ON vendor_purchase_order_item(vendor_po_id);

