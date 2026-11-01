# Database Migration Fix for Vendor PO Feature

## Issue
When starting the application, you may encounter these schema validation errors:

1. **Missing columns error:**
```
jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; 
nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [hsn_code] in table [products]
```

2. **Column type mismatch error:**
```
org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: wrong column type encountered in column [total_amount] in table [vendor_purchase_order]; found [numeric (Types#NUMERIC)], but expecting [float(53) (Types#FLOAT)]
```

## Root Cause
The Vendor PO feature implementation had two issues:

1. **Missing columns**: Added new fields to existing entities without corresponding database migrations:
   - `Product.hsnCode` and `Product.unit` fields
   - `Vendor.gstn` field

2. **Column type mismatches**: V17 migration created NUMERIC columns but Java entities use `Double` which maps to `FLOAT(53)`:
   - `vendor_purchase_order.total_amount` was `NUMERIC(14,2)` but entity expects `FLOAT(53)`
   - `vendor_purchase_order_item.quantity/unit_price/line_total` were `NUMERIC` but entity expects `FLOAT(53)`

## Solution
Created two migration files to fix the schema issues:

### V18 Migration (Already Applied)
`src/main/resources/db/migration/V18__add_product_hsn_unit_fields.sql`
- Adds missing columns: `products.hsn_code`, `products.unit`, `vendors.gstn`
- ✅ **Already applied successfully**

### V19 Migration (New)
`src/main/resources/db/migration/V19__fix_vendor_po_column_types.sql`
- Fixes column type mismatches in vendor_purchase_order tables
- Changes NUMERIC columns to FLOAT(53) to match Java Double entities

#### V19 Migration Contents:
```sql
-- Fix column type mismatches in vendor_purchase_order table
-- Java entities use Double which maps to FLOAT(53) in PostgreSQL
ALTER TABLE vendor_purchase_order ALTER COLUMN total_amount TYPE FLOAT(53);

-- Fix column types in vendor_purchase_order_item table
ALTER TABLE vendor_purchase_order_item ALTER COLUMN quantity TYPE FLOAT(53);
ALTER TABLE vendor_purchase_order_item ALTER COLUMN unit_price TYPE FLOAT(53);
ALTER TABLE vendor_purchase_order_item ALTER COLUMN line_total TYPE FLOAT(53);

-- Update the generated column definition for line_total
ALTER TABLE vendor_purchase_order_item DROP COLUMN IF EXISTS line_total;
ALTER TABLE vendor_purchase_order_item ADD COLUMN line_total FLOAT(53) GENERATED ALWAYS AS (quantity * unit_price) STORED;
```

## How to Apply
1. **If the application hasn't started yet**: The migration will run automatically on startup
2. **If the application is already running**: Restart the application to trigger Flyway migrations
3. **Manual application** (if needed): The migration can be applied manually through your database admin tool

## Verification
After migration, the application should start successfully without schema validation errors. The Vendor PO feature will then have access to:
- Product HSN codes for GST compliance
- Product units for proper quantity display
- Vendor GSTN numbers for tax documentation
- Properly typed numeric columns for calculations

## Files Modified
- `src/main/resources/db/migration/V18__add_product_hsn_unit_fields.sql` (✅ Already applied)
- `src/main/resources/db/migration/V19__fix_vendor_po_column_types.sql` (✅ Applied after cleanup)
- `src/main/resources/db/migration/V20__add_company_gstn.sql` (NEW - pending)
- Entity files updated with new fields (Product.java, Vendor.java, Company.java, VendorPo.java, VendorPoItem.java)
- UI Templates updated:
  - `templates/vendors/list.html` - Added Vendor PO tabs and GSTN column
  - `templates/vendor/pos-list.html` - Complete Vendor PO list with actions
  - `templates/orders/form.html` - Added HSN Code and Unit columns with autofill
  - `templates/admin/company/edit.html` - Added GSTN field
  - `templates/admin/company/view.html` - Added GSTN display
- Controller updated:
  - `VendorPoController.java` - Added AJAX endpoint for PO list loading

## UI Changes Implemented
✅ **Vendor Navigation**: Added Vendor List + Vendor PO tabs with AJAX loading
✅ **Vendor GSTN**: Added GSTN column to vendor list and form
✅ **Product HSN/Unit**: Added HSN Code and Unit to product form and order form with autofill
✅ **Company GSTN**: Added GSTN field to company edit/view pages
✅ **Vendor PO List**: Complete PO list with View, Download, Edit, Delete actions
✅ **Dynamic Forms**: Order form now auto-fills HSN Code and Unit when products are selected

## Next Steps
- **Start the application** to trigger V19 migration automatically
- **Verify** the application starts without schema validation errors
- **Test** the Vendor PO creation workflow
- **Confirm** calculations work properly with corrected column types
- **Verify** HSN codes and units appear correctly in PDF generation
