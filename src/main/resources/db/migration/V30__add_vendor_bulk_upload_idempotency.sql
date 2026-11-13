CREATE TABLE vendor_bulk_upload_requests (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    workbook_fingerprint VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    response_payload TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_vendor_bulk_upload_company FOREIGN KEY (company_id)
        REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT uk_vendor_bulk_upload_company_key UNIQUE (company_id, idempotency_key),
    CONSTRAINT uk_vendor_bulk_upload_company_fingerprint UNIQUE (company_id, workbook_fingerprint),
    CONSTRAINT ck_vendor_bulk_upload_status
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_vendor_bulk_upload_status_updated
    ON vendor_bulk_upload_requests(status, updated_at);
