ALTER TABLE vendor_purchase_order_item
    ADD COLUMN taxable_rate NUMERIC(14,4);

UPDATE vendor_purchase_order_item
SET taxable_rate = CASE
    WHEN pricing_unit > 0 THEN COALESCE(pre_gst_unit_price, unit_price) / pricing_unit
    ELSE 0
END;
