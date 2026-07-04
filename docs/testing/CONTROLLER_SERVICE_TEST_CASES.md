
- VPO-19 Company and Vendor entities include state field (VARCHAR(100)) for GST state comparison; Flyway migration V26 adds these columns to existing tables
- VPO-18 IndianState enum provides all 37 Indian states/union territories with GST codes (e.g., Maharashtra="27", Gujarat="24") and display names, with lookup methods romCode() and romDisplayName() (case-insensitive)
- VPO-17 PDF template conditionally displays CGST+SGST rows when isIntraState=true, displays IGST row when isIntraState=false, and shows subtotal/finalTotal in all cases
- VPO-16 VendorPdfService.generatePdf() determines intra-state (CGST+SGST) vs inter-state (IGST) by comparing company and vendor states using IndianState enum (exact match), defaults to IGST when states are null, and passes isIntraState, igst to template context
