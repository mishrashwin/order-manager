CREATE TABLE company_addresses (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    nickname VARCHAR(120) NOT NULL,
    address_line1 TEXT,
    address_line2 TEXT,
    city VARCHAR(120),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100) NOT NULL DEFAULT 'India',
    gstin VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    default_bill_to BOOLEAN NOT NULL DEFAULT FALSE,
    default_ship_to BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_company_addresses_company_active
    ON company_addresses(company_id, active);

CREATE UNIQUE INDEX uq_company_addresses_active_bill_to
    ON company_addresses(company_id)
    WHERE active = TRUE AND default_bill_to = TRUE;

CREATE UNIQUE INDEX uq_company_addresses_active_ship_to
    ON company_addresses(company_id)
    WHERE active = TRUE AND default_ship_to = TRUE;

INSERT INTO company_addresses (
    company_id, nickname, address_line1, state, country, gstin, active,
    default_bill_to, default_ship_to, created_at, updated_at
)
SELECT c.id,
       'Legacy Address',
       c.address,
       c.state,
       'India',
       c.gstn,
       TRUE,
       TRUE,
       TRUE,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM companies c;
