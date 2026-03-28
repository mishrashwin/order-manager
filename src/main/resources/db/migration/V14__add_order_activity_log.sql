-- Order activity / audit log table.
-- order_id is stored as a plain column (no FK) so activity rows survive order deletion.
CREATE TABLE order_activities (
    id             BIGSERIAL PRIMARY KEY,
    order_id       BIGINT,
    order_po_no    VARCHAR(100),
    order_client_name VARCHAR(255),
    activity_type  VARCHAR(50)  NOT NULL,
    field_changed  VARCHAR(100),
    old_value      VARCHAR(255),
    new_value      VARCHAR(255),
    description    TEXT,
    actor_username VARCHAR(100) NOT NULL,
    actor_full_name VARCHAR(255),
    activity_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    company_id     BIGINT       NOT NULL,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

CREATE INDEX idx_order_activities_company_id        ON order_activities(company_id);
CREATE INDEX idx_order_activities_activity_at       ON order_activities(activity_at);
CREATE INDEX idx_order_activities_order_id          ON order_activities(order_id);
CREATE INDEX idx_order_activities_company_act_at    ON order_activities(company_id, activity_at DESC);

