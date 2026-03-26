CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    payment_month INT NOT NULL,
    payment_year INT NOT NULL,
    payment_msg TEXT,
    payment_ss_filename VARCHAR(255),
    payment_ss_data BYTEA,
    payment_ss_content_type VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

CREATE INDEX idx_payments_company_id ON payments(company_id);
