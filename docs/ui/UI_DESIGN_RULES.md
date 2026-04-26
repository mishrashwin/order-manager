# UI DESIGN RULES

## PURPOSE
This document defines the **authoritative UI design patterns** for the Order Manager system. It ensures consistency, maintainability, and modern UX across all Thymeleaf templates.

Violation of any rule → **DO NOT MERGE**

---

# 1. UI STACK & PRINCIPLES

## 1.1 Core Stack
- **Framework**: Spring MVC + Thymeleaf (server-rendered, no SPA)
- **CSS Framework**: Bootstrap 5.3
- **Icons**: Bootstrap Icons
- **JavaScript**: Minimal, page-specific only

## 1.2 Design Principles
- Keep server-side rendering for first load
- Minimize JavaScript bundles
- Prefer Bootstrap utilities over custom CSS
- Reuse shared components and fragments
- Maintain consistent visual system across all pages

## 1.3 Performance Guardrails
- Keep first render server-side
- Avoid runtime-heavy JS
- Load JS only where needed
- Defer non-critical scripts
- Keep payloads small

---

# 2. CSS ORGANIZATION

## 2.1 Shared CSS Location
All shared styles go in: `src/main/resources/static/css/style.css`

## 2.2 Design Tokens
Use CSS custom properties in `:root` for consistency:
```css
:root {
    --om-primary: #0d6efd;
    --om-secondary: #6c757d;
    --om-success: #198754;
    --om-danger: #dc3545;
    --om-warning: #ffc107;
    --om-border-radius: 0.375rem;
    --om-box-shadow: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.075);
}
```

## 2.3 Page-Specific Scoping
Scope page-specific styles to avoid side effects:
```css
.dashboard-page { /* dashboard-specific styles */ }
.client-form-page { /* client form-specific styles */ }
```

## 2.4 Shared Component Classes
Define reusable classes for common UI patterns:
- `.om-list-toolbar` - List page toolbar
- `.om-list-toolbar-row` - Toolbar row layout
- `.om-list-actions` - Action buttons container
- `.om-list-search-input` - Search input styling
- `.om-sortable` - Sortable table headers
- `.om-action-group` - Grouped action buttons
- `.om-date-range` - Date range picker styling
- `.om-status-pill-*` - Status badge variants

## 2.5 Avoid Inline Styles
**NEVER** add inline `<style>` blocks in templates. Move to shared CSS instead.

---

# 3. THYMELEAF PATTERNS

## 3.1 Global Model Attributes
`GlobalModelAttributes` (`@ControllerAdvice` in `config/`) injects these into every view for authenticated users:
- `companyName` - Current company name
- `navbarGreetingName` - Greeting name for navbar

**CRITICAL RULE**: Do **not** re-set these attributes in new controllers. They are automatically available.

## 3.2 Fragment Reuse
Use shared fragments for repeated UI:
- `fragments/unified-navbar.html` - Navigation bar
- `fragments/footer.html` - Page footer
- `fragments/table-utils.html` - Table utilities (sort, search, filters)
- `fragments/form-actions.html` - Form action buttons

## 3.3 Template Organization
- `templates/{feature}/list.html` - List views
- `templates/{feature}/form.html` - Create forms
- `templates/{feature}/form-edit.html` - Edit forms
- `templates/{feature}/view.html` - Detail views
- `templates/{feature}/pdf/{template}.html` - PDF templates

---

# 4. LIST PAGE PATTERNS

## 4.1 Standard List Page Structure
```html
<div class="om-list-toolbar">
    <div class="om-list-toolbar-row">
        <!-- Search -->
        <div class="om-list-search-input">
            <input type="text" class="form-control" placeholder="Search...">
        </div>
        
        <!-- Filters -->
        <div class="om-date-range">
            <!-- Date range inputs -->
        </div>
        
        <!-- Actions -->
        <div class="om-list-actions">
            <a href="/feature/create" class="btn btn-primary">Add New</a>
        </div>
    </div>
</div>

<!-- Table -->
<table class="table table-hover">
    <thead>
        <tr>
            <th class="om-sortable" data-sort="name">Name</th>
            <th class="om-sortable" data-sort="status">Status</th>
            <th>Actions</th>
        </tr>
    </thead>
    <tbody>
        <tr th:each="item : ${items}">
            <td th:text="${item.name}"></td>
            <td>
                <span class="badge om-status-pill-active" th:text="${item.status}"></span>
            </td>
            <td>
                <div class="om-action-group">
                    <a th:href="@{/feature/{id}(id=${item.id})}" class="btn btn-sm btn-outline-primary">View</a>
                    <a th:href="@{/feature/{id}/edit(id=${item.id})}" class="btn btn-sm btn-outline-secondary">Edit</a>
                </div>
            </td>
        </tr>
    </tbody>
</table>
```

## 4.2 Status Badge Classes
Use Bootstrap badge classes with custom variants:
- `.om-status-pill-active` - Green for active status
- `.om-status-pill-inactive` - Gray for inactive status
- `.om-status-pill-pending` - Yellow for pending status
- `.om-status-pill-final` - Blue for final/completed status

---

# 5. FORM PAGE PATTERNS

## 5.1 Standard Form Structure
```html
<form th:action="@{/feature/save}" th:object="${dto}" method="post">
    <!-- CSRF token automatically included -->
    
    <div class="mb-3">
        <label for="name" class="form-label">Name</label>
        <input type="text" class="form-control" id="name" th:field="*{name}" required>
        <div class="invalid-feedback" th:if="${#fields.hasErrors('name')}" th:errors="*{name}"></div>
    </div>
    
    <div class="mb-3">
        <label for="email" class="form-label">Email</label>
        <input type="email" class="form-control" id="email" th:field="*{email}" required>
        <div class="invalid-feedback" th:if="${#fields.hasErrors('email')}" th:errors="*{email}"></div>
    </div>
    
    <div class="om-action-group">
        <button type="submit" class="btn btn-primary">Save</button>
        <a href="/feature/list" class="btn btn-secondary">Cancel</a>
    </div>
</form>
```

## 5.2 Error Handling
Use Bootstrap validation classes:
- `.is-invalid` - Invalid input styling
- `.invalid-feedback` - Error message display
- `.d-block` - Force display of feedback

Example:
```html
<div class="mb-3">
    <label for="mobile" class="form-label">Mobile</label>
    <input type="tel" class="form-control" 
           id="mobile" 
           th:field="*{mobile}"
           th:classappend="${mobileError} ? 'is-invalid' : ''">
    <div class="invalid-feedback d-block" th:if="${mobileError}" th:text="${mobileError}"></div>
</div>
```

## 5.3 Dynamic Row Addition
For forms with dynamic rows (e.g., PO line items):
- Use JavaScript to add/remove rows
- Maintain proper Thymeleaf field naming for binding
- Example: Vendor PO form with dynamic item rows

---

# 6. DASHBOARD PATTERNS

## 6.1 Drag-and-Drop Status Board
Dashboard uses Sortable.js for drag-and-drop status updates:
```javascript
// Initialize Sortable
new Sortable(document.getElementById('order-board'), {
    animation: 150,
    onEnd: function(evt) {
        // Persist status change via API
        const orderId = evt.item.dataset.orderId;
        const newStatus = evt.to.dataset.status;
        axios.patch(`/api/orders/${orderId}`, { status: newStatus });
    }
});
```

## 6.2 Status Update API
- Endpoint: `PATCH /api/orders/{id}`
- Payload: `{ status: '<ENUM_NAME>' }`
- Status values must be enum names (`OrderStatus.name()`)
- UI reverts card if PATCH fails

## 6.3 Date Range Defaults
Dashboard and order list pages default to past 1 month when `startDate`/`endDate` query params are absent.

---

# 7. AUTH PAGE PATTERNS

## 7.1 Login Page Layout
- Use split layout on desktop (left: branding, right: form)
- Centered layout on mobile
- Right-side placement for login form to match company registration

## 7.2 Auth Page Classes
```html
<body class="auth-page auth-page-split auth-login-right">
```

## 7.3 Mobile Responsiveness
```css
@media (max-width: 768px) {
    .auth-page-split {
        flex-direction: column;
    }
    .auth-login-right {
        justify-content: center !important;
        padding-right: 0 !important;
    }
}
```

---

# 8. JAVASCRIPT USAGE

## 8.1 Minimal JS Principle
Only use JavaScript when necessary:
- Dashboard drag-and-drop (Sortable.js)
- AJAX status updates (axios)
- Dynamic form row addition
- Real-time calculations (e.g., PO totals)

## 8.2 Library Usage
- **Sortable.js** - Drag-and-drop on dashboard
- **axios** - AJAX requests for API endpoints
- **Tom Select** (optional) - Searchable selects on heavy forms
- **Chart.js** (optional) - Only on pages that truly need charts

## 8.3 Script Placement
Load scripts at the end of body or defer:
```html
<script defer src="/js/dashboard.js"></script>
```

---

# 9. PDF TEMPLATES

## 9.1 PDF Template Location
`src/main/resources/templates/{feature}/pdf/{template}.html`

## 9.2 PDF Template Patterns
- Use OpenHTMLToPDF for PDF generation
- Pass computed values from service (e.g., GST calculations)
- Format numbers with comma separators
- Handle intra-state (CGST+SGST) vs inter-state (IGST) display

## 9.3 Number Formatting
Use `DecimalFormat("#,##0.00")` for all numeric values in PDFs.

---

# 10. ENFORCEMENT CHECKLIST

Before merge:

- [ ] No inline `<style>` blocks in templates
- [ ] Page-specific styles scoped with page class
- [ ] Shared component classes used for repeated patterns
- [ ] GlobalModelAttributes not re-set in controllers
- [ ] Fragments used for repeated UI elements
- [ ] Bootstrap utilities preferred over custom CSS
- [ ] JavaScript minimal and page-specific only
- [ ] Forms use proper validation classes
- [ ] Status badges use consistent pill classes
- [ ] Mobile responsiveness tested

---

# FINAL RULE

This document is enforceable.

If any guideline is violated:
👉 REJECT PR
