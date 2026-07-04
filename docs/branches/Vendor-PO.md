# Branch: Vendor-PO

## Intent
Enhance Vendor PO feature with advanced GST calculations and multi-vendor support.
Additionally, resolve critical CSRF session expiration issue affecting user login experience.
**GST Calculation Reversal**: Reverse GST calculation approach from accepting GST-inclusive prices to accepting pre-GST prices
and calculating GST-inclusive amounts across Product, Order, and Vendor PO modules.
**Product Selection Improvements**: Fix product selection behavior to ensure fresh rows show "Select Product" as default and clear auto-populated fields when product is deselected.
**Vendor PO Edit Form Fixes**: Apply column width optimizations and product selection improvements to Vendor PO edit form to match creation form behavior.
**Add Existing Product Button Fix**: Fix broken "Add Existing Product" button in both Vendor PO creation and edit forms.
**Order Product Layout Consistency**: Fix Product layout inconsistency between Order Create and Order Edit forms to ensure identical column arrangement.
**Order Form Header Layout**: Combine Customer Name and PO Order No fields into a single row with 3:1 ratio for better space utilization.
**Vendor PO Form Enhancement**: Add "Add Vendor" button to Vendor PO form to match Order form's "Add Client" functionality.
**Edit Mode Add Existing Product Fix**: Fix "Add Existing Product" button not working in Edit Vendor PO mode by enhancing fallback logic to remove selected attributes from copied options.

## Work Done

### [2026-07-04] Documentation alignment
**Problem**: The project README still described the older MySQL/Gmail runtime and Swagger defaults, which no longer matched the committed profiles and security/email configuration.

**Solution Implemented**:
- Updated README runtime references to PostgreSQL, Brevo email delivery, owner approval flow, and disabled Swagger UI defaults.
- Updated the agent notes so future work starts from the current profile and security behavior.

**Outcome**:
- Repository-facing docs now match the active dev/qa/prod configuration and login flow.

### [2026-05-09] Edit Mode Add Existing Product Fix
**Problem**: "Add Existing Product" button was not working on Edit Vendor PO, but it was working on Create Vendor PO. Initial fix caused Create mode to show multiple empty product rows due to draft restoration issues.

**Root Cause Identified**:
- In Edit mode, when `window.productData` was not available or empty, the fallback logic copied options from existing server-rendered selects
- The copied options included `selected` attributes from existing items, causing newly added rows to inherit incorrect selections
- This made the dropdown appear with a pre-selected value instead of showing the default "-- Select Product --" state
- Draft restoration in Create mode was causing multiple empty rows to appear due to stale draft data

**Solution Implemented**:
- Enhanced the fallback logic (line 300) to remove `selected` attributes when copying options from existing selects using regex: `existingSelect.innerHTML.replace(/\s*selected\s*=\s*["']?selected["']?/gi, '')`
- Modified draft clearing logic (lines 628-632) to clear draft in Create mode to prevent restoring stale data that could cause multiple empty rows
- This ensures that when a new row is added via "Add Existing Product" button in Edit mode, it always starts with a clean, unselected state matching Create mode behavior
- Create mode now starts fresh without stale draft data, preventing the multiple empty rows issue

**Key Technical Changes**:
```javascript
// Fallback logic with selected attribute removal
optionsHtml = existingSelect.innerHTML.replace(/\s*selected\s*=\s*["']?selected["']?/gi, '');

// Draft clearing in Create mode
if (!isEditMode) {
    sessionStorage.removeItem(draftKey);
}
```

**Files Modified**:
- `src/main/resources/templates/vendor/pos-form-new.html`

### [2026-05-09] Edit Mode Remove Button Text Fix
**Problem**: In Create Vendor PO, the remove button showed both the icon and "Remove" text, but in Edit Vendor PO, the remove button only showed the icon without text.

**Root Cause Identified**:
- The server-rendered existing items in Edit mode (line 164-166) had the remove button without text
- The default empty row in Create mode (line 215-217) and JavaScript ROW_TEMPLATE (line 378-380) had the remove button with "Remove" text
- This caused inconsistency between Create and Edit modes

**Solution Implemented**:
- Added " Remove" text to the Edit mode remove button (line 165) to match Create mode and ROW_TEMPLATE behavior
- Now all remove buttons consistently show both the icon and "Remove" text across both Create and Edit modes

**Key Technical Changes**:
```html
<!-- Before (Edit mode) -->
<button type="button" class="btn btn-outline-danger btn-sm remove-product-row">
  <i class="bi bi-dash-circle"></i>
</button>

<!-- After (Edit mode) -->
<button type="button" class="btn btn-outline-danger btn-sm remove-product-row">
  <i class="bi bi-dash-circle"></i> Remove
</button>
```

**Files Modified**:
- `src/main/resources/templates/vendor/pos-form-new.html`

### [2026-05-09] Vendor PO Form Layout Ratio Update
**Problem**: Vendor PO Create and Edit forms had Vendor and PO Number fields in a 50:50 column ratio (col-md-6 each), which was inconsistent with the Order form's 75:25 ratio.

**Root Cause Identified**:
- Vendor form used col-md-6 for both Vendor and PO Number fields
- Order form uses col-md-9 for Customer and col-md-3 for PO/Order No (75:25 ratio)
- This inconsistency created different layout patterns between similar forms

**Solution Implemented**:
- Changed Vendor field from col-md-6 to col-md-9 (75% width)
- Changed PO Number field from col-md-6 to col-md-3 (25% width)
- Now Vendor PO form matches Order form's 75:25 layout ratio for consistency

**Key Technical Changes**:
```html
<!-- Before -->
<div class="col-md-6 mb-4">Vendor</div>
<div class="col-md-6 mb-4">PO Number</div>

<!-- After -->
<div class="col-md-9 mb-4">Vendor</div>
<div class="col-md-3 mb-4">PO Number</div>
```

**Files Modified**:
- `src/main/resources/templates/vendor/pos-form-new.html`

### [2026-05-09] Vendor PO Product Column Order Update
**Problem**: Vendor PO Create and Edit forms had Unit and Qty columns in the wrong order (Unit before Qty), which was inconsistent with the Order form (Qty before Unit).

**Root Cause Identified**:
- Vendor PO form had column order: Product → HSN → Unit → Qty → Unit Price → GST % → Total → Remove
- Order form had column order: Product → HSN → Qty → Unit → Unit Price → GST % → GST-Inclusive Price → Total → Remove
- This inconsistency created different user experience patterns between similar forms

**Solution Implemented**:
- Swapped Unit and Qty column order in all three sections:
  - Edit mode (th:each block for existing items)
  - Create mode (th:if block for default empty row)
  - JavaScript ROW_TEMPLATE (for dynamically added rows)
- Now Vendor PO form matches Order form's column order for consistency

**Key Technical Changes**:
```html
<!-- Before -->
<div class="col-md-1">
    <input type="text" name="itemUnits" placeholder="Unit">
</div>
<div class="col-md-1">
    <input type="number" name="itemQuantities" placeholder="Qty">
</div>

<!-- After -->
<div class="col-md-1">
    <input type="number" name="itemQuantities" placeholder="Qty">
</div>
<div class="col-md-1">
    <input type="text" name="itemUnits" placeholder="Unit">
</div>
```

**Files Modified**:
- `src/main/resources/templates/vendor/pos-form-new.html`

### [2026-05-09] Vendor PO Form Header Layout Optimization
**Problem**: Vendor PO form had Delivery Date and Email Recipients in 50:50 ratio on one line, with Linked Order PO Numbers on a separate line, which was inefficient use of space.

**Root Cause Identified**:
- Delivery Date and Email Recipients occupied 50:50 ratio on one line
- Linked Order PO Numbers occupied full width on a separate line
- This layout wasted vertical space and could be optimized to a single row

**Solution Implemented**:
- Combined all three fields into a single row with 25:25:50 ratio
- Linked Order PO Numbers: col-md-3 (25% width)
- Delivery Date: col-md-3 (25% width)
- Email Recipients: col-md-6 (50% width)
- Improved form layout efficiency and space utilization

**Key Technical Changes**:
```html
<!-- Before -->
<div class="row">
  <div class="col-md-6">Delivery Date</div>
  <div class="col-md-6">Email Recipients</div>
</div>
<div class="row">
  <div class="col-md-12">Linked Order PO Numbers</div>
</div>

<!-- After -->
<div class="row">
  <div class="col-md-3">Linked Order PO Numbers</div>
  <div class="col-md-3">Delivery Date</div>
  <div class="col-md-6">Email Recipients</div>
</div>
```

**Files Modified**:
- `src/main/resources/templates/vendor/pos-form-new.html`

### [2026-05-03] Remove "Incl. GST" from Vendor PO Form
**Problem**: The "Incl. GST" column was present in the Vendor PO form (`pos-form-new.html`) but not in the Create/Edit Order page (`orders/form.html`), leading to inconsistency in the UI.

**Root Cause Identified**:
- The Vendor PO form's product item rows included a dedicated column for "Incl. GST" which was not part of the Order form's design.
- This created an unnecessary visual difference and potentially redundant information display.

**Solution Implemented**:
- Removed the `div` element corresponding to the "Incl. GST" column from both the `th:each` block (for existing items) and the `ROW_TEMPLATE` (for new items) in `pos-form-new.html`.
- Adjusted the Bootstrap column widths of the remaining elements to maintain a 12-column grid. The "Total" column's width was increased from `col-md-1` to `col-md-2` to compensate for the removed `col-md-1` "Incl. GST" column.

**New Column Layout (12-column total)**:
- Product Select (`col-md-3`)
- HSN Code (`col-md-1`)
- Unit (`col-md-1`)
- Quantity (`col-md-1`)
- Pre-GST Unit Price (`col-md-2`)
- GST % (`col-md-1`)
- Final Line Total (`col-md-2`)
- Remove Button (`col-md-1`)

**Key Technical Changes**:
```html
<!-- Before (example for existing items) -->
<div class="col-md-1">
  <div class="input-group">
    <span class="input-group-text">₹</span>
    <input type="text" class="form-control form-control-custom bg-light-subtle"
           readonly placeholder="Incl. GST"
           th:value="${item.gstInclusiveUnitPrice != null ? #numbers.formatDecimal(item.gstInclusiveUnitPrice, 1, 2) : (item.unitPrice != null ? #numbers.formatDecimal(item.unitPrice, 1, 2) : '')}">
  </div>
</div>
<div class="col-md-1"> <!-- This was the 'Total' column -->
  <div class="input-group">
    <span class="input-group-text">₹</span>
    <input type="text" class="form-control form-control-custom line-total-display"
           readonly placeholder="Total"
           th:value="${item.lineTotal != null ? #numbers.formatDecimal(item.lineTotal, 1, 2) : '0.00'}">
  </div>
</div>

<!-- After (example for existing items) -->
<!-- Removed the 'Incl. GST' column -->
<div class="col-md-2"> <!-- 'Total' column width increased to col-md-2 -->
  <div class="input-group">
    <span class="input-group-text">₹</span>
    <input type="text" class="form-control form-control-custom line-total-display"
           readonly placeholder="Total"
           th:value="${item.lineTotal != null ? #numbers.formatDecimal(item.lineTotal, 1, 2) : '0.00'}">
  </div>
</div>
```

```javascript
// JavaScript recalc function updates
// Removed logic related to updating input[placeholder="Incl. GST"]
// The lineTotal calculation remains the same, as it uses gstInclusiveUnitPrice internally.
```

**Files Modified**:
- `src/main/resources/templates/vendor/pos-form-new.html`

### [2026-05-03] Vendor PO Form Layout and "Add Existing Product" Fix
**Problem**: The "Add Existing Product" functionality on the Vendor PO page was not working correctly, and the layout/spacing of product rows was inconsistent between existing items (rendered via `th:each`) and newly added items (rendered via `ROW_TEMPLATE`). Specifically, the column sum for existing items was 14, exceeding Bootstrap's 12-column grid, leading to layout issues.

**Root Cause Identified**:
- Discrepancy in Bootstrap column classes between the `th:each` block (for existing items) and the `ROW_TEMPLATE` (for new items).
- The `th:each` block had a total column sum of 14 (`col-md-3 + col-md-1 + col-md-1 + col-md-1 + col-md-1 + col-md-1 + col-md-2 + col-md-2 + col-md-1 = 14`), causing layout overflow.
- The `ROW_TEMPLATE` was also slightly inconsistent and missing the "Remove" button text.
- The `recalc` JavaScript function needed adjustments to correctly target the "GST-Inclusive Price" and "Total" fields based on the new layout.

**Solution Implemented**:
- Standardized the Bootstrap column layout for all product rows (both `th:each` and `ROW_TEMPLATE`) to a 12-column grid.
- Consolidated the "GST Amount" display into the "GST-Inclusive Price" field to reduce column count.
- Adjusted column widths to ensure all necessary fields are visible and aligned.
- Updated the `th:each` block to reflect the new 12-column structure and removed the redundant "GST Amount" column.
- Updated the `ROW_TEMPLATE` to match the new 12-column structure, ensuring all fields are present and the "Remove" button includes its text.
- Refined the `recalc` JavaScript function to correctly calculate and update the "GST-Inclusive Price" and "Final Line Total" based on the updated field structure and placeholders.

**New Column Layout (12-column total)**:
- Product Select (`col-md-3`)
- HSN Code (`col-md-1`)
- Unit (`col-md-1`)
- Quantity (`col-md-1`)
- Pre-GST Unit Price (`col-md-2`)
- GST % (`col-md-1`)
- GST-Inclusive Unit Price (`col-md-1`)
- Final Line Total (`col-md-1`)
- Remove Button (`col-md-1`)

**Key Technical Changes**:
```html
<!-- Before (example for existing items - total 14 columns) -->
<div class="col-md-2">
  <div class="input-group">
    <span class="input-group-text">₹</span>
    <input type="text" class="form-control form-control-custom bg-light-subtle"
           readonly placeholder="Line Total"
           th:value="${item.gstAmount != null ? #numbers.formatDecimal(item.gstAmount, 1, 2) : '0.00'}">
  </div>
</div>
<div class="col-md-2">
  <div class="input-group">
    <span class="input-group-text">₹</span>
    <input type="text" class="form-control form-control-custom bg-light-subtle"
           readonly placeholder="GST-Inclusive Price"
           th:value="${item.gstInclusiveUnitPrice != null ? #numbers.formatDecimal(item.gstInclusiveUnitPrice, 1, 2) : (item.unitPrice != null ? #numbers.formatDecimal(item.unitPrice, 1, 2) : '')}">
  </div>
</div>
<div class="col-md-1">
  <div class="input-group">
    <span class="input-group-text">₹</span>
    <input type="text" class="form-control form-control-custom line-total-display"
           readonly placeholder="Total"
           th:value="${item.lineTotal != null ? #numbers.formatDecimal(item.lineTotal, 1, 2) : '0.00'}">
  </div>
</div>

<!-- After (example for existing items - total 12 columns) -->
<div class="col-md-1">
  <div class="input-group">
    <span class="input-group-text">₹</span>
    <input type="text" class="form-control form-control-custom bg-light-subtle"
           readonly placeholder="Incl. GST"
           th:value="${item.gstInclusiveUnitPrice != null ? #numbers.formatDecimal(item.gstInclusiveUnitPrice, 1, 2) : (item.unitPrice != null ? #numbers.formatDecimal(item.unitPrice, 1, 2) : '')}">
  </div>
</div>
<div class="col-md-1">
  <div class="input-group">
    <span class="input-group-text">₹</span>
    <input type="text" class="form-control form-control-custom line-total-display"
           readonly placeholder="Total"
           th:value="${item.lineTotal != null ? #numbers.formatDecimal(item.lineTotal, 1, 2) : '0.00'}">
  </div>
</div>
<div class="col-md-1">
  <button type="button" class="btn btn-outline-danger btn-sm remove-product-row">
    <i class="bi bi-dash-circle"></i>
  </button>
</div>
```

```javascript
// JavaScript recalc function updates
// Targeting input[placeholder="Incl. GST"] for GST-inclusive unit price
// Targeting input[placeholder="Total"] for final line total
```

**Files Modified**:
- `src/main/resources/templates/vendor/pos-form-new.html`

### [2026-05-03] Vendor PO Edit-mode product selection parity fix
**Problem**: After initial fixes, adding existing products worked correctly in Create Vendor PO mode but the Edit Vendor PO form still exhibited incorrect behavior: newly added rows in edit mode sometimes inherited incorrect option attributes or showed the previously-selected proxy object rather than the raw `productId` value, causing the dropdown to appear empty or auto-select unexpectedly.

**Root Cause Identified**:
- Server-rendered edit rows used `item.productId` correctly, but the client-side JS (ROW_TEMPLATE) and product options generation differed in attribute shape and in the timing of when `cachedProductOptions` was applied to existing selects.
- Some global AJAX link handlers interfered with navigation from the Vendor PO list to the edit page in certain environments, leading to partial initialization of page-scoped JS.

**Solution Implemented**:
- Ensure all product select elements use the same `name` attribute (`itemProductIds`) and that server-side `th:selected` uses `item.productId` (plain Long) — this avoids lazy proxy comparison issues.
- Cache product option HTML at page load into `cachedProductOptions` and use it for dynamically added rows.
- When rendering existing rows server-side, include the same `data-*` attributes (`data-price`, `data-hsn`, `data-unit`, `data-gst`) on each option so client-side auto-population is available.
- In the client-side `createRow` routine, explicitly set `select.value = ''` when no productId is provided to guarantee the default "-- Select Product --" state.
- Defensive UI: mark action links that must navigate away from AJAX fragments with `data-no-ajax="true"` (see `pos-list-ajax.html`) so full-page JS initialization runs on the edit page.

**Files Modified**:
- `src/main/resources/templates/vendor/pos-form-new.html` (JS: cachedProductOptions init, createRow select reset, consistent data-* attributes)
- `src/main/resources/templates/vendor/pos-list-ajax.html` (mark edit links to avoid AJAX interception)


### [2026-05-03] Order Form Header Layout Optimization
**Problem**: Customer Name and PO Order No fields were on separate lines in the Order form, wasting vertical space and reducing form efficiency.

**Root Cause Identified**:
- Customer Name field occupied full width (col-md-12 equivalent) on its own line
- PO Order No field occupied full width on the next line
- No horizontal space utilization for these related header fields

**Solution Implemented**:
- Combined Customer Name and PO Order No fields into a single Bootstrap row
- Applied 3:1 column ratio (col-md-9 for Customer Name, col-md-3 for PO Order No)
- Maintained all existing functionality (client selection, add client button, validation)
- Improved form layout efficiency and space utilization

**Key Technical Changes**:
```html
<!-- Before -->
<div class="mb-4">
    <label>Customer Name</label>
    <select>...</select>
</div>
<div class="mb-4">
    <label>PO/Order No</label>
    <input>...</input>
</div>

<!-- After -->
<div class="row mb-4">
    <div class="col-md-9">
        <label>Customer Name</label>
        <select>...</select>
    </div>
    <div class="col-md-3">
        <label>PO/Order No</label>
        <input>...</input>
    </div>
</div>
```

**Files Modified**:
- `src/main/resources/templates/orders/form.html`

### [2026-05-03] Vendor PO Form Enhancement - Add Vendor Button
**Problem**: Vendor PO form lacked an "Add Vendor" button, forcing users to navigate away manually to add a new vendor before creating a PO. This was inconsistent with the Order form which has an "Add Client" button.

**Root Cause Identified**:
- Vendor select field was not wrapped in an input-group
- No button/link to add new vendor directly from the PO form
- Missing JavaScript handler to save draft before navigating away
- Inconsistent UX compared to Order form's "Add Client" functionality

**Solution Implemented**:
- Wrapped vendor select in Bootstrap input-group
- Added "Add Vendor" button with link to `/vendors/new`
- Included returnTo parameter to redirect back to Vendor PO form after adding vendor
- Added JavaScript handler to save draft before navigating away
- Added helper text: "If the desired vendor is missing, click 'Add Vendor'."
- Maintained consistency with Order form's "Add Client" pattern

**Key Technical Changes**:
```html
<!-- Before -->
<select class="form-select form-control-custom" th:field="*{vendor.id}" required>
    <option value="">-- Select Vendor --</option>
    ...
</select>

<!-- After -->
<div class="input-group">
    <select class="form-select form-control-custom" th:field="*{vendor.id}" required>
        <option value="">-- Select Vendor --</option>
        ...
    </select>
    <a id="addVendorLink" class="btn btn-outline-success"
       th:href="@{/vendors/new(returnTo=${vendorPo.id == null ? '/vendor/pos/new' : '/vendor/pos/' + vendorPo.id + '/edit'})}">
        <i class="bi bi-plus-circle"></i> Add Vendor
    </a>
</div>
<small class="form-text text-muted">If the desired vendor is missing, click "Add Vendor".</small>
```

```javascript
// JavaScript handler for draft saving
const addVendorLink = document.getElementById('addVendorLink');
if (addVendorLink) {
    addVendorLink.addEventListener('click', function (e) {
        e.preventDefault();
        saveDraft();
        window.location.href = this.href;
    });
}
```

**Files Modified**:
- `src/main/resources/templates/vendor/pos-form-new.html`

### [2026-05-01] Order Product Layout Consistency Fix
**Problem**: Order Create and Order Edit forms showed inconsistent placeholder text in the Pre-GST Price field, causing visual inconsistency in the Product layout. Additionally, when clicking "Add Existing Product" in Edit Order mode, a product was automatically selected by default instead of showing no selection.

**Root Cause Identified**:
- Edit mode (existing items section) used placeholder "Unit Price"
- Create mode (default empty row section) used placeholder "Pre-GST Price"
- JavaScript template for dynamically added rows used placeholder "Pre-GST Price"
- Pre-GST Price field width was too narrow (col-md-1) for better usability
- Remove button width was inconsistent (col-md-2 vs col-md-1) across sections
- JavaScript `createProductRow` function did not explicitly reset select value when adding new rows in Edit mode

**Solution Implemented**:
- Updated all sections to use consistent "Unit Price" placeholder in `orders/form.html`
- Increased Pre-GST Price field width from col-md-1 to col-md-2 in all three sections
- Changed Remove button width from col-md-2 to col-md-1 in all three sections for consistency
- Added explicit `newSelect.value = ''` in `createProductRow` function when no productId is provided
- Now all three sections (Edit mode, Create mode, JavaScript template) use consistent "Unit Price" placeholder, field width, and button width
- Column layouts are now identical across Create and Edit modes with improved field sizing
- "Add Existing Product" button now correctly shows no product selected in both Create and Edit modes

**Key Technical Changes**:
```html
<!-- Before (Edit mode) -->
<div class="col-md-1">
    <input type="number" placeholder="Unit Price" required>
</div>
<div class="col-md-2">
    <button type="button" class="btn btn-outline-danger btn-sm remove-product-row">
        <i class="bi bi-dash-circle"></i> Remove
    </button>
</div>

<!-- After (Edit mode) -->
<div class="col-md-2">
    <input type="number" placeholder="Unit Price" required>
</div>
<div class="col-md-1">
    <button type="button" class="btn btn-outline-danger btn-sm remove-product-row">
        <i class="bi bi-dash-circle"></i> Remove
    </button>
</div>
```

```javascript
// JavaScript fix
if (newSelect) {
    newSelect.innerHTML = productOptionsHtml || getProductOptions();
    if (rowData && rowData.productId) {
        newSelect.value = rowData.productId;
    } else {
        newSelect.value = ''; // Explicitly reset to empty selection
    }
}
```

### [2026-04-26] CSRF Session Expiration Fix
**Problem**: Users experiencing "Access Restricted" error when attempting to login after extended periods. Only resolved after manual logout and re-login.

**Root Cause Identified**: 
- CSRF tokens expire after inactivity while login page remains open
- Form contains stale CSRF token that no longer matches session
- Spring Security throws CsrfException → AccessDeniedHandler → access-denied page

**Solution Implemented**:

#### Configuration Updates:
- **application-dev.properties**: Added session timeout (30m), CSRF repository config
- **application-prod.properties**: Added session timeout (30m), secure cookies, CSRF repository config  
- **application-qa.properties**: Added session timeout (30m), CSRF repository config

#### Security Enhancement:
- **SecurityConfig.java**: Enhanced AccessDeniedHandler to detect login-related CSRF failures
- Redirects login CSRF errors back to login page instead of generic access-denied
- Added specific error handling for `/login` endpoint CSRF failures

#### Frontend Improvements:
- **login.html**: Added JavaScript for CSRF token management
- Idle detection: Auto-refresh page if idle > 20 minutes before form submission
- Auto CSRF refresh: Token refresh every 15 minutes during active use
- Enhanced error messages: Specific handling for `csrf-expired` parameter
- User-friendly messaging with clock icon and clear instructions

#### Key Technical Changes:
```java
// SecurityConfig.java - Enhanced AccessDeniedHandler
if ("/login".equals(requestURI) || (referer != null && referer.contains("/login"))) {
    response.sendRedirect("/login?error&reason=csrf-expired");
    return;
}
```

```javascript
// login.html - CSRF refresh logic
if (idleTimeMinutes > 20) {
    e.preventDefault();
    submitBtn.innerHTML = '<i class="bi bi-arrow-clockwise me-1"></i> Refreshing...';
    window.location.reload();
}
```

**Files Modified**:
- `src/main/resources/application-dev.properties`
- `src/main/resources/application-prod.properties` 
- `src/main/resources/application-qa.properties`
- `src/main/java/com/example/ordermanager/config/SecurityConfig.java`
- `src/main/resources/templates/auth/login.html`

### [2026-04-26] GST Calculation Reversal Implementation
**Problem**: System was designed to accept GST-inclusive prices and derive pre-GST values for calculations. User requested reversal to accept pre-GST prices and calculate GST-inclusive prices.

**Solution Implemented**:

#### Database Schema Changes:
- **V27__add_pre_gst_price_fields.sql**: Added pre-GST price fields to products, order_items, vendor_purchase_order_item tables
- Migration includes backward compatibility by calculating pre-GST prices from existing GST-inclusive prices
- **PostgreSQL Compatibility Fix**: Replaced `ROUND()` function with `CAST(... AS DECIMAL(12,2))` for PostgreSQL compatibility

#### Backend Entity Updates:
- **Product.java**: Added preGstPrice, gstInclusivePrice fields with automatic GST calculation methods
- **OrderItem.java**: Added preGstUnitPrice, gstInclusiveUnitPrice, gstAmount fields with calculation logic  
- **VendorPoItem.java**: Added preGstUnitPrice, gstInclusiveUnitPrice fields
- **VendorPo.java**: Updated recalcTotal() method to use pre-GST price calculation approach

#### Service Layer Updates:
- **ProductService.java**: Updated to handle pre-GST price input and calculate GST-inclusive prices
- **OrderService.java**: Modified syncDerivedFieldsFromItems() to calculate GST-inclusive prices from pre-GST prices
- **VendorPoServiceImpl.java**: Updated resolveItemProduct() to handle pre-GST price conversion for backward compatibility

#### UI Form Updates:
- **Product Form**: Changed input from "Price" to "Pre-GST Price" with automatic GST-inclusive price display
- **Order Form**: Updated to accept "Pre-GST Price" and display calculated "GST-Inclusive Price"
- **Vendor PO Form**: Modified to use "Pre-GST Price" input with automatic GST calculation
- **Thymeleaf Template Fix**: Fixed `#numbers.formatDecimal()` method calls to use correct parameter format (minIntegerDigits, maxFractionDigits) instead of (maxFractionDigits)

#### Controller Layer Updates:
- **VendorPoController.java**: Updated create and update methods to handle `itemPreGstUnitPrices` instead of `itemUnitPrices`
- **VendorPoController.java**: Updated `buildVendorPoItems` method to process pre-GST prices and set `preGstUnitPrice` on items
- **Vendor PO List Page Fix**: Resolved issue where product column was empty after updating Vendor PO due to parameter name mismatch

#### Enhanced Product Tabs:
- **Order Form**: Enhanced Product tab with detailed columns in requested order: Product Name, HSN Code, Quantity, Unit, Pre-GST Price, GST%, Line Total (GST Amount = GST-Inclusive Price - Pre-GST Price)
- **Vendor PO Form**: Enhanced Product tab with detailed columns in requested order: Product Name, HSN Code, Quantity, Unit, Pre-GST Price, GST%, Line Total (GST Amount = GST-Inclusive Price - Pre-GST Price)
- **Column Reordering**: Reordered both forms to match user-specified column sequence for better workflow
- **Column Width Optimization**: Reduced column widths for HSN Code, Quantity, Unit, and Pre-GST Price to prevent unnecessary line breaks
- **GST Amount Calculation**: Added real-time GST amount calculation and display in both forms (Line Total = GST-Inclusive Price - Pre-GST Price)
- **Line Total Calculation Fix**: Fixed Line Total to show full line total (Quantity × GST-Inclusive Price) instead of just GST amount
- **Line Total Clearing**: Added logic to clear Line Total when quantity or pre-GST price is zero/empty
- **JavaScript Updates**: Enhanced `recalculateOrderSummary` and `recalc` functions to calculate and display Line Total values dynamically

#### Product Selection Improvements:
- **Default Selection**: Fresh product rows now show "Select Product" option as default in both Order and Vendor PO forms
- **Field Clearing**: Auto-populated fields (HSN Code, Unit, GST%, Pre-GST Price) are cleared when product is changed back to "Select Product"
- **Consistent Behavior**: Both Order and Vendor PO forms now have identical product selection behavior
- **Enhanced UX**: Users can easily reset product selection without manual field clearing

#### Vendor PO Edit Form Fixes:
- **Column Width Consistency**: Applied column width optimizations to Vendor PO edit form to match creation form
- **HSN Code**: Reduced from col-md-2 to col-md-1 in edit form
- **Quantity**: Reduced from col-md-2 to col-md-1 in edit form
- **Product Selection**: Product selection improvements now work consistently in both creation and edit forms

#### Add Existing Product Button Fix:
- **Root Cause**: `getProductOptions()` function returned empty string when no existing rows were present
- **Solution**: Implemented product options caching mechanism to store initial product options
- **Implementation**: Added `cachedProductOptions` variable and initialization code to cache options on page load
- **Fixed Forms**: Both Vendor PO creation and edit forms now have working "Add Existing Product" buttons

#### JavaScript Debugging Fixes:
- **Variable Declaration Order**: Fixed `cachedProductOptions` being used before declaration
- **Product Data Access**: Added embedded JavaScript data using Thymeleaf for product options generation
- **Data Attributes**: Ensured proper data attributes (data-price, data-hsn, data-unit, data-gst) are included in generated options
- **Auto-population**: Fixed product field auto-population by ensuring data attributes are available for new rows

#### Key Technical Changes:
```java
// Product.java - GST calculation method
public void calculateGstInclusivePrice() {
    if (preGstPrice != null && gstPercentage != null && gstPercentage.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal gstMultiplier = gstPercentage.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        gstInclusivePrice = preGstPrice.multiply(BigDecimal.ONE.add(gstMultiplier))
            .setScale(2, RoundingMode.HALF_UP);
        this.price = gstInclusivePrice.doubleValue();
    }
}
```

```javascript
// UI Forms - GST calculation
function calculateGstInclusivePrice() {
    const preGstPrice = parseFloat(preGstPriceInput.value) || 0;
    const gstPercentage = parseFloat(gstPercentageInput.value) || 0;
    
    if (preGstPrice > 0 && gstPercentage > 0) {
        const gstInclusivePrice = preGstPrice * (1 + gstPercentage / 100);
        gstInclusiveAmount.textContent = gstInclusivePrice.toFixed(2);
    }
}
```

## Key Decisions
- **Session timeout**: 30 minutes across all environments for consistency
- **Cookie security**: Production uses secure=true, QA/Dev use secure=false
- **Auto-refresh strategy**: 15-minute intervals with visibility/focus detection
- **Error handling**: Smart detection of login vs non-login CSRF failures
- **User experience**: Clear messaging and automatic token refresh

## Outcomes
- ✅ CSRF token expiration issue resolved across all environments
- ✅ Users can now login successfully after extended inactivity periods
- ✅ No more "Access Restricted" page for login-related CSRF failures
- ✅ Automatic token refresh provides seamless user experience
- ✅ Production-ready security configuration with proper HTTPS cookie handling
- ✅ GST calculation reversal implemented across all modules (Product, Order, Vendor PO)
- ✅ Users now input pre-GST prices instead of GST-inclusive prices
- ✅ Automatic GST-inclusive price calculation implemented in all forms
- ✅ Database migration ensures backward compatibility with existing data
- ✅ All UI forms updated with real-time GST calculation display
- ✅ Backend services updated to handle new pricing approach
- ✅ System compiled successfully with new GST calculation logic
- ✅ Order Product layout consistency fixed - placeholder text now identical across Create and Edit modes
- ✅ Vendor PO form enhanced with "Add Vendor" button matching Order form's "Add Client" functionality
- ✅ Vendor PO form layout and "Add Existing Product" functionality now consistent and working correctly.
- ✅ Product row layout on Vendor PO page now adheres to Bootstrap's 12-column grid.
- ✅ Inconsistent spacing and overflow issues in Vendor PO product rows are resolved.
- ✅ "Add Existing Product" button now correctly adds new product rows with proper layout and calculations.
- ✅ "Incl. GST" column successfully removed from Vendor PO form for UI consistency with Order form.

## Files Modified

### CSRF Session Fix (Previous Work)
- `src/main/resources/application-dev.properties`
- `src/main/resources/application-prod.properties` 
- `src/main/resources/application-qa.properties`
- `src/main/java/com/example/ordermanager/config/SecurityConfig.java`
- `src/main/resources/templates/auth/login.html`

### GST Calculation Reversal Implementation
- `src/main/resources/db/migration/V27__add_pre_gst_price_fields.sql`
- `src/main/java/com/example/ordermanager/product/entity/Product.java`
- `src/main/java/com/example/ordermanager/order/entity/OrderItem.java`
- `src/main/java/com/example/ordermanager/vendor/entity/VendorPoItem.java`
- `src/main/java/com/example/ordermanager/vendor/entity/VendorPo.java`
- `src/main/java/com/example/ordermanager/product/service/ProductService.java`
- `src/main/java/com/example/ordermanager/order/service/OrderService.java`
- `src/main/java/com/example/ordermanager/vendor/service/impl/VendorPoServiceImpl.java`
- `src/main/java/com/example/ordermanager/vendor/controller/VendorPoController.java`
- `src/main/resources/templates/products/form.html`
- `src/main/resources/templates/orders/form.html` (Enhanced with GST Amount column, fixed placeholder consistency)
- `src/main/resources/templates/vendor/pos-form-new.html` (Enhanced with GST Amount column)

### [2026-04-26] GST Calculation Reversal Implementation
**Problem**: System was designed to accept GST-inclusive prices and derive pre-GST values for calculations. User requested reversal to accept pre-GST prices and calculate GST-inclusive prices.

**Solution Implemented**:

#### Database Schema Changes:
- **V27__add_pre_gst_price_fields.sql**: Added pre-GST price fields to products, order_items, vendor_purchase_order_item tables
- Migration includes backward compatibility by calculating pre-GST prices from existing GST-inclusive prices
- **PostgreSQL Compatibility Fix**: Replaced `ROUND()` function with `CAST(... AS DECIMAL(12,2))` for PostgreSQL compatibility

#### Backend Entity Updates:
- **Product.java**: Added preGstPrice, gstInclusivePrice fields with automatic GST calculation methods
- **OrderItem.java**: Added preGstUnitPrice, gstInclusiveUnitPrice, gstAmount fields with calculation logic  
- **VendorPoItem.java**: Added preGstUnitPrice, gstInclusiveUnitPrice fields
- **VendorPo.java**: Updated recalcTotal() method to use pre-GST price calculation approach

#### Service Layer Updates:
- **ProductService.java**: Updated to handle pre-GST price input and calculate GST-inclusive prices
- **OrderService.java**: Modified syncDerivedFieldsFromItems() to calculate GST-inclusive prices from pre-GST prices
- **VendorPoServiceImpl.java**: Updated resolveItemProduct() to handle pre-GST price conversion for backward compatibility

#### UI Form Updates:
- **Product Form**: Changed input from "Price" to "Pre-GST Price" with automatic GST-inclusive price display
- **Order Form**: Updated to accept "Pre-GST Price" and display calculated "GST-Inclusive Price"
- **Vendor PO Form**: Modified to use "Pre-GST Price" input with automatic GST calculation
- **Thymeleaf Template Fix**: Fixed `#numbers.formatDecimal()` method calls to use correct parameter format (minIntegerDigits, maxFractionDigits) instead of (maxFractionDigits)

#### Controller Layer Updates:
- **VendorPoController.java**: Updated create and update methods to handle `itemPreGstUnitPrices` instead of `itemUnitPrices`
- **VendorPoController.java**: Updated `buildVendorPoItems` method to process pre-GST prices and set `preGstUnitPrice` on items
- **Vendor PO List Page Fix**: Resolved issue where product column was empty after updating Vendor PO due to parameter name mismatch

#### Enhanced Product Tabs:
- **Order Form**: Enhanced Product tab with detailed columns in requested order: Product Name, HSN Code, Quantity, Unit, Pre-GST Price, GST%, Line Total (GST Amount = GST-Inclusive Price - Pre-GST Price)
- **Vendor PO Form**: Enhanced Product tab with detailed columns in requested order: Product Name, HSN Code, Quantity, Unit, Pre-GST Price, GST%, Line Total (GST Amount = GST-Inclusive Price - Pre-GST Price)
- **Column Reordering**: Reordered both forms to match user-specified column sequence for better workflow
- **Column Width Optimization**: Reduced column widths for HSN Code, Quantity, Unit, and Pre-GST Price to prevent unnecessary line breaks
- **GST Amount Calculation**: Added real-time GST amount calculation and display in both forms (Line Total = GST-Inclusive Price - Pre-GST Price)
- **Line Total Calculation Fix**: Fixed Line Total to show full line total (Quantity × GST-Inclusive Price) instead of just GST amount
- **Line Total Clearing**: Added logic to clear Line Total when quantity or pre-GST price is zero/empty
- **JavaScript Updates**: Enhanced `recalculateOrderSummary` and `recalc` functions to calculate and display Line Total values dynamically

#### Product Selection Improvements:
- **Default Selection**: Fresh product rows now show "Select Product" option as default in both Order and Vendor PO forms
- **Field Clearing**: Auto-populated fields (HSN Code, Unit, GST%, Pre-GST Price) are cleared when product is changed back to "Select Product"
- **Consistent Behavior**: Both Order and Vendor PO forms now have identical product selection behavior
- **Enhanced UX**: Users can easily reset product selection without manual field clearing

#### Vendor PO Edit Form Fixes:
- **Column Width Consistency**: Applied column width optimizations to Vendor PO edit form to match creation form
- **HSN Code**: Reduced from col-md-2 to col-md-1 in edit form
- **Quantity**: Reduced from col-md-2 to col-md-1 in edit form
- **Product Selection**: Product selection improvements now work consistently in both creation and edit forms

#### Add Existing Product Button Fix:
- **Root Cause**: `getProductOptions()` function returned empty string when no existing rows were present
- **Solution**: Implemented product options caching mechanism to store initial product options
- **Implementation**: Added `cachedProductOptions` variable and initialization code to cache options on page load
- **Fixed Forms**: Both Vendor PO creation and edit forms now have working "Add Existing Product" buttons

#### JavaScript Debugging Fixes:
- **Variable Declaration Order**: Fixed `cachedProductOptions` being used before declaration
- **Product Data Access**: Added embedded JavaScript data using Thymeleaf for product options generation
- **Data Attributes**: Ensured proper data attributes (data-price, data-hsn, data-unit, data-gst) are included in generated options
- **Auto-population**: Fixed product field auto-population by ensuring data attributes are available for new rows

#### Key Technical Changes:
```java
// Product.java - GST calculation method
public void calculateGstInclusivePrice() {
    if (preGstPrice != null && gstPercentage != null && gstPercentage.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal gstMultiplier = gstPercentage.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        gstInclusivePrice = preGstPrice.multiply(BigDecimal.ONE.add(gstMultiplier))
            .setScale(2, RoundingMode.HALF_UP);
        this.price = gstInclusivePrice.doubleValue();
    }
}
```

```javascript
// UI Forms - GST calculation
function calculateGstInclusivePrice() {
    const preGstPrice = parseFloat(preGstPriceInput.value) || 0;
    const gstPercentage = parseFloat(gstPercentageInput.value) || 0;
    
    if (preGstPrice > 0 && gstPercentage > 0) {
        const gstInclusivePrice = preGstPrice * (1 + gstPercentage / 100);
        gstInclusiveAmount.textContent = gstInclusivePrice.toFixed(2);
    }
}
```

## Testing Notes
- Tested idle detection with 20+ minute page load times
- Verified CSRF token refresh functionality
- Confirmed production security settings (secure cookies)
- Validated error message handling for different failure scenarios
- ✅ GST calculation reversal compiled successfully
- ✅ All UI forms updated with pre-GST price inputs
- ✅ Backend services updated for new calculation approach
- ✅ Database migration includes backward compatibility
- ✅ PostgreSQL compatibility issue resolved - replaced ROUND() with CAST() for proper DECIMAL handling
- ✅ Migration script now works with both PostgreSQL and MySQL syntax
- ✅ Thymeleaf template errors fixed - corrected #numbers.formatDecimal() parameter format
- ✅ All forms (Order, Vendor PO) now display properly without template parsing errors
- ✅ Vendor PO list page product column issue resolved - fixed parameter name mismatch in controller
- ✅ Vendor PO items now properly display product names in list view after save/update operations
- ✅ Product selection behavior now consistent across Order and Vendor PO forms
- ✅ Fresh product rows show "Select Product" as default option
- ✅ Auto-populated fields clear properly when product is deselected
- ✅ Column layout optimized to prevent unnecessary line breaks
- ✅ Line Total calculations show correct full line total amounts
- ✅ Line Total fields clear appropriately when inputs are zero/empty
- ✅ Vendor PO edit form now has consistent column widths with creation form
- ✅ Column width optimizations applied to both Vendor PO creation and edit forms
- ✅ Product selection improvements now work consistently in Vendor PO edit forms
- ✅ "Add Existing Product" button now working in both Vendor PO creation and edit forms
- ✅ Product options caching mechanism implemented to handle empty row scenarios
- ✅ All Vendor PO form functionality now matches Order form behavior
- ✅ JavaScript variable declaration order fixed to prevent undefined errors
- ✅ Embedded product data implemented for proper product options generation
- ✅ Data attributes properly included in generated product options for auto-population
- ✅ Product field auto-population now working correctly in Vendor PO forms
- ✅ "Add Existing Product" button functionality fully restored in both creation and edit forms
- ✅ Enhanced Product tabs implemented in Order and Vendor PO forms with detailed columns
- ✅ GST Amount column added to both forms with real-time calculation (GST Amount = GST-Inclusive Price - Pre-GST Price)
- ✅ Product tabs now show: Product Name, HSN Code, Quantity, Unit, Pre-GST Price, GST-Inclusive Price, GST%, GST Amount
- ✅ JavaScript functions updated to calculate and display GST amounts dynamically in both forms
- ✅ Order form header layout optimized - Customer Name and PO Order No now on same line with 3:1 ratio
- ✅ Vendor PO form enhanced with "Add Vendor" button matching Order form's "Add Client" functionality
- ✅ Vendor PO form layout and "Add Existing Product" functionality now consistent and working correctly.
- ✅ Product row layout on Vendor PO page now adheres to Bootstrap's 12-column grid.
- ✅ Inconsistent spacing and overflow issues in Vendor PO product rows are resolved.
- ✅ "Add Existing Product" button now correctly adds new product rows with proper layout and calculations.

Summary
Vendor PO branch: GST calculation reversal, UI consistency fixes, and form enhancements

GST Calculation Reversal:
- Reverse GST calculation from GST-inclusive to pre-GST pricing across Product, Order, and Vendor PO modules
- Add pre-gst price fields to database schema (V27 migration) with backward compatibility
- Update entities (Product, OrderItem, VendorPoItem) with automatic GST calculation methods
- Modify service layer to handle pre-GST price input and calculate GST-inclusive prices
- Update UI forms to accept "Pre-GST Price" input with automatic GST-inclusive display

Vendor PO Form Enhancements:
- Fix "Add Existing Product" button in Edit mode by removing selected attributes from copied options
- Clear draft data in Create mode to prevent stale data from causing multiple empty rows
- Add "Remove" text to Edit mode remove button for consistency with Create mode
- Update Vendor/PO Number layout ratio from 50:50 to 75:25 to match Order form
- Add "Add Vendor" button to Vendor PO form with draft saving before navigation
- Remove "Incl. GST" column for UI consistency with Order form
- Standardize product row layout to 12-column Bootstrap grid
- Fix product selection parity between Create and Edit modes

Order Form Improvements:
- Combine Customer Name and PO Order No fields into single row with 3:1 ratio
- Fix Product layout consistency between Create and Edit modes
- Ensure "Add Existing Product" shows no selection in both modes

Security Fixes:
- Fix CSRF session expiration issue with enhanced AccessDeniedHandler
- Add session timeout configuration (30m) across all environments
- Implement frontend CSRF token refresh with idle detection
- Add specific error handling for login-related CSRF failures

Files Modified:
- src/main/resources/templates/vendor/pos-form-new.html
- src/main/resources/templates/vendor/pos-list-ajax.html
- src/main/resources/templates/orders/form.html
- src/main/resources/templates/auth/login.html
- src/main/resources/application-dev.properties
- src/main/resources/application-prod.properties
- src/main/resources/application-qa.properties
- src/main/resources/db/migration/V27__add_pre_gst_price_fields.sql
- src/main/java/com/example/ordermanager/product/entity/Product.java
- src/main/java/com/example/ordermanager/order/entity/OrderItem.java
- src/main/java/com/example/ordermanager/vendor/entity/VendorPoItem.java
- src/main/java/com/example/ordermanager/vendor/entity/VendorPo.java
- src/main/java/com/example/ordermanager/product/service/ProductService.java
- src/main/java/com/example/ordermanager/order/service/OrderService.java
- src/main/java/com/example/ordermanager/vendor/service/impl/VendorPoServiceImpl.java
- src/main/java/com/example/ordermanager/config/SecurityConfig.java
