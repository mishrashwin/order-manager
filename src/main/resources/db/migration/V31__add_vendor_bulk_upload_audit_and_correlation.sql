ALTER TABLE vendor_bulk_upload_requests
    ADD COLUMN uploaded_filename VARCHAR(255) NOT NULL DEFAULT 'unknown.xlsx';
ALTER TABLE vendor_bulk_upload_requests
    ADD COLUMN initiated_by VARCHAR(255) NOT NULL DEFAULT 'unknown';
ALTER TABLE vendor_bulk_upload_requests
    ADD COLUMN manifest_payload TEXT;
ALTER TABLE vendor_bulk_upload_requests
    ADD COLUMN last_failure_details TEXT;
ALTER TABLE vendor_bulk_upload_requests
    ADD COLUMN total_parsed_rows INTEGER;
ALTER TABLE vendor_bulk_upload_requests
    ADD COLUMN valid_row_count INTEGER;
ALTER TABLE vendor_bulk_upload_requests
    ADD COLUMN registered_row_count INTEGER;
ALTER TABLE vendor_bulk_upload_requests
    ADD COLUMN failed_row_count INTEGER;
ALTER TABLE vendor_bulk_upload_requests
    ADD COLUMN skipped_row_count INTEGER;
ALTER TABLE vendor_bulk_upload_requests
    ADD COLUMN reconciliation_state VARCHAR(30) NOT NULL DEFAULT 'NOT_REQUIRED';
ALTER TABLE vendor_bulk_upload_requests
    ADD COLUMN reconciled_at TIMESTAMP;

ALTER TABLE vendor_bulk_upload_requests
    DROP CONSTRAINT ck_vendor_bulk_upload_status;

ALTER TABLE vendor_bulk_upload_requests
    ADD CONSTRAINT ck_vendor_bulk_upload_status
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED', 'RECONCILIATION_REQUIRED'));

ALTER TABLE vendor_bulk_upload_requests
    ADD CONSTRAINT ck_vendor_bulk_upload_reconciliation_state
        CHECK (reconciliation_state IN ('NOT_REQUIRED', 'FULLY_COMMITTED',
            'PARTIALLY_COMMITTED', 'NO_RECORDS_COMMITTED', 'INCONCLUSIVE'));

ALTER TABLE vendors
    ADD COLUMN bulk_upload_request_id BIGINT;
ALTER TABLE vendors ADD COLUMN bulk_upload_row_number INTEGER;
ALTER TABLE vendors ADD COLUMN bulk_upload_processed_at TIMESTAMP;
ALTER TABLE vendors
    ADD CONSTRAINT fk_vendor_bulk_upload_request FOREIGN KEY (bulk_upload_request_id)
        REFERENCES vendor_bulk_upload_requests(id);
ALTER TABLE vendors
    ADD CONSTRAINT uk_vendor_bulk_upload_request_row
        UNIQUE (bulk_upload_request_id, bulk_upload_row_number);

CREATE INDEX idx_vendor_bulk_upload_request_status_updated
    ON vendor_bulk_upload_requests(company_id, status, updated_at);

CREATE INDEX idx_vendors_bulk_upload_request
    ON vendors(company_id, bulk_upload_request_id);
