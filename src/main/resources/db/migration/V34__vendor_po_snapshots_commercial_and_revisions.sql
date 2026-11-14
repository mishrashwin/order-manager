ALTER TABLE vendors ADD COLUMN default_payment_terms TEXT;

CREATE TABLE vendor_po_terms_template (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    name VARCHAR(160) NOT NULL,
    terms_json TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_vendor_po_terms_template_company_name UNIQUE (company_id, name)
);
CREATE INDEX idx_vendor_po_terms_template_company_active
    ON vendor_po_terms_template(company_id, active);

CREATE TABLE vendor_po_delivery_term (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    name VARCHAR(160) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_vendor_po_delivery_term_company_name UNIQUE (company_id, name)
);
CREATE INDEX idx_vendor_po_delivery_term_company_active
    ON vendor_po_delivery_term(company_id, active);

ALTER TABLE vendor_purchase_order
    DROP CONSTRAINT IF EXISTS ux_vendor_po_company_po_number;
DROP INDEX IF EXISTS ux_vendor_po_company_po_number;

ALTER TABLE vendor_purchase_order
    ADD COLUMN po_date DATE,
    ADD COLUMN revision_number INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN root_po_id BIGINT,
    ADD COLUMN previous_revision_id BIGINT,
    ADD COLUMN current_revision BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN revision_created_at TIMESTAMPTZ,
    ADD COLUMN revision_created_by VARCHAR(128),
    ADD COLUMN snapshot_origin VARCHAR(32),
    ADD COLUMN issued_pdf BYTEA,
    ADD COLUMN bill_to_address_id BIGINT REFERENCES company_addresses(id),
    ADD COLUMN ship_to_address_id BIGINT REFERENCES company_addresses(id),
    ADD COLUMN bill_to_nickname VARCHAR(120),
    ADD COLUMN bill_to_address_line1 TEXT,
    ADD COLUMN bill_to_address_line2 TEXT,
    ADD COLUMN bill_to_city VARCHAR(120),
    ADD COLUMN bill_to_state VARCHAR(100),
    ADD COLUMN bill_to_state_code VARCHAR(2),
    ADD COLUMN bill_to_postal_code VARCHAR(20),
    ADD COLUMN bill_to_country VARCHAR(100),
    ADD COLUMN bill_to_gstin VARCHAR(50),
    ADD COLUMN bill_to_drug_license_number VARCHAR(100),
    ADD COLUMN ship_to_nickname VARCHAR(120),
    ADD COLUMN ship_to_address_line1 TEXT,
    ADD COLUMN ship_to_address_line2 TEXT,
    ADD COLUMN ship_to_city VARCHAR(120),
    ADD COLUMN ship_to_state VARCHAR(100),
    ADD COLUMN ship_to_state_code VARCHAR(2),
    ADD COLUMN ship_to_postal_code VARCHAR(20),
    ADD COLUMN ship_to_country VARCHAR(100),
    ADD COLUMN ship_to_gstin VARCHAR(50),
    ADD COLUMN ship_to_drug_license_number VARCHAR(100),
    ADD COLUMN buyer_company_name VARCHAR(255),
    ADD COLUMN buyer_gstin VARCHAR(50),
    ADD COLUMN buyer_logo_data BYTEA,
    ADD COLUMN buyer_logo_content_type VARCHAR(100),
    ADD COLUMN buyer_signature_data BYTEA,
    ADD COLUMN buyer_signature_content_type VARCHAR(100),
    ADD COLUMN vendor_contact_person VARCHAR(255),
    ADD COLUMN vendor_address TEXT,
    ADD COLUMN vendor_email VARCHAR(255),
    ADD COLUMN vendor_phone VARCHAR(50),
    ADD COLUMN vendor_state VARCHAR(100),
    ADD COLUMN vendor_state_code VARCHAR(2),
    ADD COLUMN payment_terms TEXT,
    ADD COLUMN delivery_terms TEXT,
    ADD COLUMN delivery_term_id BIGINT REFERENCES vendor_po_delivery_term(id),
    ADD COLUMN freight_included BOOLEAN,
    ADD COLUMN freight_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN freight_taxable BOOLEAN,
    ADD COLUMN freight_gst_percentage NUMERIC(5,2),
    ADD COLUMN packing_included BOOLEAN,
    ADD COLUMN packing_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN packing_taxable BOOLEAN,
    ADD COLUMN packing_gst_percentage NUMERIC(5,2),
    ADD COLUMN penalty_clause TEXT,
    ADD COLUMN terms_template_id BIGINT REFERENCES vendor_po_terms_template(id),
    ADD COLUMN terms_conditions_snapshot TEXT,
    ADD COLUMN terms_conditions_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN subtotal_before_tax NUMERIC(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN total_tax NUMERIC(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN cgst_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN sgst_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN igst_amount NUMERIC(14,2) NOT NULL DEFAULT 0;

UPDATE vendor_purchase_order
SET po_date = COALESCE(created_at::date, CURRENT_DATE),
    root_po_id = id,
    snapshot_origin = 'LEGACY_BACKFILL';

ALTER TABLE vendor_purchase_order
    ADD CONSTRAINT fk_vendor_po_root FOREIGN KEY (root_po_id) REFERENCES vendor_purchase_order(id),
    ADD CONSTRAINT fk_vendor_po_previous_revision FOREIGN KEY (previous_revision_id)
        REFERENCES vendor_purchase_order(id);

CREATE UNIQUE INDEX ux_vendor_po_company_number_revision
    ON vendor_purchase_order(company_id, po_number, revision_number);
CREATE INDEX idx_vendor_po_root_revision
    ON vendor_purchase_order(root_po_id, revision_number);

WITH source AS (
    SELECT po.id AS po_id, c.name AS buyer_company_name,
        COALESCE(bill.gstin, c.gstn) AS buyer_gstin,
        c.logo_data AS buyer_logo_data, c.logo_content_type AS buyer_logo_content_type,
        c.signature_data AS buyer_signature_data,
        c.signature_content_type AS buyer_signature_content_type,
        bill.id AS bill_to_address_id, bill.nickname AS bill_to_nickname,
        bill.address_line1 AS bill_to_address_line1, bill.address_line2 AS bill_to_address_line2,
        bill.city AS bill_to_city, bill.state AS bill_to_state,
        CASE bill.state
        WHEN 'ANDAMAN_AND_NICOBAR_ISLANDS' THEN '35' WHEN 'ANDHRA_PRADESH' THEN '37'
        WHEN 'ARUNACHAL_PRADESH' THEN '12' WHEN 'ASSAM' THEN '18' WHEN 'BIHAR' THEN '10'
        WHEN 'CHANDIGARH' THEN '04' WHEN 'CHHATTISGARH' THEN '22'
        WHEN 'DADRA_AND_NAGAR_HAVELI_AND_DAMAN_AND_DIU' THEN '26' WHEN 'DELHI' THEN '07'
        WHEN 'GOA' THEN '30' WHEN 'GUJARAT' THEN '24' WHEN 'HARYANA' THEN '06'
        WHEN 'HIMACHAL_PRADESH' THEN '02' WHEN 'JAMMU_AND_KASHMIR' THEN '01'
        WHEN 'JHARKHAND' THEN '20' WHEN 'KARNATAKA' THEN '29' WHEN 'KERALA' THEN '32'
        WHEN 'LADAKH' THEN '38' WHEN 'LAKSHADWEEP' THEN '31' WHEN 'MADHYA_PRADESH' THEN '23'
        WHEN 'MAHARASHTRA' THEN '27' WHEN 'MANIPUR' THEN '14' WHEN 'MEGHALAYA' THEN '17'
        WHEN 'MIZORAM' THEN '15' WHEN 'NAGALAND' THEN '13' WHEN 'ODISHA' THEN '21'
        WHEN 'OTHER_TERRITORY' THEN '97' WHEN 'PUDUCHERRY' THEN '34' WHEN 'PUNJAB' THEN '03'
        WHEN 'RAJASTHAN' THEN '08' WHEN 'SIKKIM' THEN '11' WHEN 'TAMIL_NADU' THEN '33'
        WHEN 'TELANGANA' THEN '36' WHEN 'TRIPURA' THEN '16' WHEN 'UTTARAKHAND' THEN '05'
        WHEN 'UTTAR_PRADESH' THEN '09' WHEN 'WEST_BENGAL' THEN '19' ELSE NULL END AS bill_to_state_code,
        bill.postal_code AS bill_to_postal_code, bill.country AS bill_to_country,
        bill.gstin AS bill_to_gstin, bill.drug_license_number AS bill_to_drug_license_number,
        ship.id AS ship_to_address_id, ship.nickname AS ship_to_nickname,
        ship.address_line1 AS ship_to_address_line1, ship.address_line2 AS ship_to_address_line2,
        ship.city AS ship_to_city, ship.state AS ship_to_state,
        CASE ship.state
        WHEN 'ANDAMAN_AND_NICOBAR_ISLANDS' THEN '35' WHEN 'ANDHRA_PRADESH' THEN '37'
        WHEN 'ARUNACHAL_PRADESH' THEN '12' WHEN 'ASSAM' THEN '18' WHEN 'BIHAR' THEN '10'
        WHEN 'CHANDIGARH' THEN '04' WHEN 'CHHATTISGARH' THEN '22'
        WHEN 'DADRA_AND_NAGAR_HAVELI_AND_DAMAN_AND_DIU' THEN '26' WHEN 'DELHI' THEN '07'
        WHEN 'GOA' THEN '30' WHEN 'GUJARAT' THEN '24' WHEN 'HARYANA' THEN '06'
        WHEN 'HIMACHAL_PRADESH' THEN '02' WHEN 'JAMMU_AND_KASHMIR' THEN '01'
        WHEN 'JHARKHAND' THEN '20' WHEN 'KARNATAKA' THEN '29' WHEN 'KERALA' THEN '32'
        WHEN 'LADAKH' THEN '38' WHEN 'LAKSHADWEEP' THEN '31' WHEN 'MADHYA_PRADESH' THEN '23'
        WHEN 'MAHARASHTRA' THEN '27' WHEN 'MANIPUR' THEN '14' WHEN 'MEGHALAYA' THEN '17'
        WHEN 'MIZORAM' THEN '15' WHEN 'NAGALAND' THEN '13' WHEN 'ODISHA' THEN '21'
        WHEN 'OTHER_TERRITORY' THEN '97' WHEN 'PUDUCHERRY' THEN '34' WHEN 'PUNJAB' THEN '03'
        WHEN 'RAJASTHAN' THEN '08' WHEN 'SIKKIM' THEN '11' WHEN 'TAMIL_NADU' THEN '33'
        WHEN 'TELANGANA' THEN '36' WHEN 'TRIPURA' THEN '16' WHEN 'UTTARAKHAND' THEN '05'
        WHEN 'UTTAR_PRADESH' THEN '09' WHEN 'WEST_BENGAL' THEN '19' ELSE NULL END AS ship_to_state_code,
        ship.postal_code AS ship_to_postal_code, ship.country AS ship_to_country,
        ship.gstin AS ship_to_gstin, ship.drug_license_number AS ship_to_drug_license_number,
        v.contact_person AS vendor_contact_person, v.address AS vendor_address,
        v.email AS vendor_email, v.phone AS vendor_phone, v.state AS vendor_state,
        CASE v.state
        WHEN 'ANDAMAN_AND_NICOBAR_ISLANDS' THEN '35' WHEN 'ANDHRA_PRADESH' THEN '37'
        WHEN 'ARUNACHAL_PRADESH' THEN '12' WHEN 'ASSAM' THEN '18' WHEN 'BIHAR' THEN '10'
        WHEN 'CHANDIGARH' THEN '04' WHEN 'CHHATTISGARH' THEN '22'
        WHEN 'DADRA_AND_NAGAR_HAVELI_AND_DAMAN_AND_DIU' THEN '26' WHEN 'DELHI' THEN '07'
        WHEN 'GOA' THEN '30' WHEN 'GUJARAT' THEN '24' WHEN 'HARYANA' THEN '06'
        WHEN 'HIMACHAL_PRADESH' THEN '02' WHEN 'JAMMU_AND_KASHMIR' THEN '01'
        WHEN 'JHARKHAND' THEN '20' WHEN 'KARNATAKA' THEN '29' WHEN 'KERALA' THEN '32'
        WHEN 'LADAKH' THEN '38' WHEN 'LAKSHADWEEP' THEN '31' WHEN 'MADHYA_PRADESH' THEN '23'
        WHEN 'MAHARASHTRA' THEN '27' WHEN 'MANIPUR' THEN '14' WHEN 'MEGHALAYA' THEN '17'
        WHEN 'MIZORAM' THEN '15' WHEN 'NAGALAND' THEN '13' WHEN 'ODISHA' THEN '21'
        WHEN 'OTHER_TERRITORY' THEN '97' WHEN 'PUDUCHERRY' THEN '34' WHEN 'PUNJAB' THEN '03'
        WHEN 'RAJASTHAN' THEN '08' WHEN 'SIKKIM' THEN '11' WHEN 'TAMIL_NADU' THEN '33'
        WHEN 'TELANGANA' THEN '36' WHEN 'TRIPURA' THEN '16' WHEN 'UTTARAKHAND' THEN '05'
        WHEN 'UTTAR_PRADESH' THEN '09' WHEN 'WEST_BENGAL' THEN '19' ELSE NULL END AS vendor_state_code,
        COALESCE(c.vendor_po_terms_conditions, '[]') AS terms_conditions_snapshot,
        COALESCE(c.vendor_po_terms_conditions_enabled, TRUE) AS terms_conditions_enabled
    FROM vendor_purchase_order po
    JOIN companies c ON c.id = po.company_id
    LEFT JOIN vendors v ON v.id = po.vendor_id
    LEFT JOIN company_addresses bill ON bill.company_id = c.id AND bill.active AND bill.default_bill_to
    LEFT JOIN company_addresses ship ON ship.company_id = c.id AND ship.active AND ship.default_ship_to
)
UPDATE vendor_purchase_order p SET
    buyer_company_name = s.buyer_company_name, buyer_gstin = s.buyer_gstin,
    buyer_logo_data = s.buyer_logo_data, buyer_logo_content_type = s.buyer_logo_content_type,
    buyer_signature_data = s.buyer_signature_data,
    buyer_signature_content_type = s.buyer_signature_content_type,
    bill_to_address_id = s.bill_to_address_id, bill_to_nickname = s.bill_to_nickname,
    bill_to_address_line1 = s.bill_to_address_line1, bill_to_address_line2 = s.bill_to_address_line2,
    bill_to_city = s.bill_to_city, bill_to_state = s.bill_to_state,
    bill_to_state_code = s.bill_to_state_code, bill_to_postal_code = s.bill_to_postal_code,
    bill_to_country = s.bill_to_country, bill_to_gstin = s.bill_to_gstin,
    bill_to_drug_license_number = s.bill_to_drug_license_number,
    ship_to_address_id = s.ship_to_address_id, ship_to_nickname = s.ship_to_nickname,
    ship_to_address_line1 = s.ship_to_address_line1, ship_to_address_line2 = s.ship_to_address_line2,
    ship_to_city = s.ship_to_city, ship_to_state = s.ship_to_state,
    ship_to_state_code = s.ship_to_state_code, ship_to_postal_code = s.ship_to_postal_code,
    ship_to_country = s.ship_to_country, ship_to_gstin = s.ship_to_gstin,
    ship_to_drug_license_number = s.ship_to_drug_license_number,
    vendor_contact_person = s.vendor_contact_person, vendor_address = s.vendor_address,
    vendor_email = s.vendor_email, vendor_phone = s.vendor_phone, vendor_state = s.vendor_state,
    vendor_state_code = s.vendor_state_code, terms_conditions_snapshot = s.terms_conditions_snapshot,
    terms_conditions_enabled = s.terms_conditions_enabled
FROM source s WHERE s.po_id = p.id;

