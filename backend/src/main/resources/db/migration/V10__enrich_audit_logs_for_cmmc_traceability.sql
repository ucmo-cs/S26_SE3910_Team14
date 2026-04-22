ALTER TABLE audit_logs
    ADD COLUMN actor_type VARCHAR(16),
    ADD COLUMN actor_employee_id BIGINT,
    ADD COLUMN actor_username VARCHAR(128),
    ADD COLUMN actor_email VARCHAR(255),
    ADD COLUMN actor_role VARCHAR(128),
    ADD COLUMN request_method VARCHAR(16),
    ADD COLUMN request_path VARCHAR(255),
    ADD COLUMN user_agent VARCHAR(512);

ALTER TABLE audit_logs
    ADD CONSTRAINT fk_audit_logs_actor_employee
        FOREIGN KEY (actor_employee_id) REFERENCES employees (id);

CREATE INDEX idx_audit_logs_actor_employee ON audit_logs (actor_employee_id);
CREATE INDEX idx_audit_logs_request_path ON audit_logs (request_path);
