-- Keep counters per company and prefix because company names (and their initials) can change.
CREATE TABLE vendor_po_number_sequence (
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    prefix VARCHAR(100) NOT NULL,
    last_value BIGINT NOT NULL CHECK (last_value >= 0),
    PRIMARY KEY (company_id, prefix)
);

-- Seed every numeric prefix found in existing records, including revision rows and gaps.
INSERT INTO vendor_po_number_sequence (company_id, prefix, last_value)
SELECT company_id, captures[1], MAX(captures[2]::BIGINT)
FROM (
    SELECT company_id, regexp_match(po_number, '^(.+?)([0-9]+)$') AS captures
    FROM vendor_purchase_order
    WHERE po_number IS NOT NULL
) existing
WHERE captures IS NOT NULL
GROUP BY company_id, captures[1]
ON CONFLICT (company_id, prefix) DO UPDATE
SET last_value = GREATEST(vendor_po_number_sequence.last_value, EXCLUDED.last_value);
