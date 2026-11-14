ALTER TABLE vendor_purchase_order
    ALTER COLUMN total_amount TYPE NUMERIC(14,2)
        USING ROUND(total_amount::numeric, 2);

ALTER TABLE vendor_purchase_order_item
    ADD COLUMN line_total_decimal NUMERIC(14,2),
    ADD COLUMN pricing_unit NUMERIC(12,3) NOT NULL DEFAULT 1,
    ADD COLUMN taxable_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN cgst_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN sgst_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN igst_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN delivery_date DATE;

UPDATE vendor_purchase_order_item
SET line_total_decimal = ROUND(line_total::numeric, 2);

ALTER TABLE vendor_purchase_order_item DROP COLUMN line_total;
ALTER TABLE vendor_purchase_order_item RENAME COLUMN line_total_decimal TO line_total;
ALTER TABLE vendor_purchase_order_item
    ALTER COLUMN quantity TYPE NUMERIC(12,3) USING ROUND(quantity::numeric, 3),
    ALTER COLUMN unit_price TYPE NUMERIC(14,4) USING ROUND(unit_price::numeric, 4);

UPDATE vendor_purchase_order_item i
SET taxable_amount = COALESCE(
        i.pre_gst_unit_price * i.quantity / NULLIF(i.pricing_unit, 0),
        i.line_total - COALESCE(i.gst_amount, 0),
        i.line_total),
    gst_amount = COALESCE(i.gst_amount,
        i.line_total - COALESCE(i.pre_gst_unit_price * i.quantity
            / NULLIF(i.pricing_unit, 0), i.line_total)),
    cgst_amount = CASE WHEN p.bill_to_state_code IS NOT NULL
        AND p.bill_to_state_code = p.vendor_state_code
        THEN ROUND(COALESCE(i.gst_amount, 0) / 2, 2) ELSE 0 END,
    sgst_amount = CASE WHEN p.bill_to_state_code IS NOT NULL
        AND p.bill_to_state_code = p.vendor_state_code
        THEN COALESCE(i.gst_amount, 0) - ROUND(COALESCE(i.gst_amount, 0) / 2, 2)
        ELSE 0 END,
    igst_amount = CASE WHEN p.bill_to_state_code IS NULL
        OR p.bill_to_state_code <> p.vendor_state_code
        THEN COALESCE(i.gst_amount, 0) ELSE 0 END
FROM vendor_purchase_order p
WHERE p.id = i.vendor_po_id;

UPDATE vendor_purchase_order p
SET subtotal_before_tax = COALESCE(t.subtotal, 0),
    total_tax = COALESCE(t.tax, 0),
    cgst_amount = COALESCE(t.cgst, 0),
    sgst_amount = COALESCE(t.sgst, 0),
    igst_amount = COALESCE(t.igst, 0),
    total_amount = COALESCE(t.subtotal, 0) + COALESCE(t.tax, 0)
FROM (
    SELECT vendor_po_id,
        SUM(taxable_amount) AS subtotal,
        SUM(gst_amount) AS tax,
        SUM(cgst_amount) AS cgst,
        SUM(sgst_amount) AS sgst,
        SUM(igst_amount) AS igst
    FROM vendor_purchase_order_item
    GROUP BY vendor_po_id
) t
WHERE t.vendor_po_id = p.id;
