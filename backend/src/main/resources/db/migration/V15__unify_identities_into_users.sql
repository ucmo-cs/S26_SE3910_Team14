CREATE TABLE IF NOT EXISTS users (
    id                     BIGSERIAL PRIMARY KEY,
    branch_id              BIGINT REFERENCES branches (id),
    role_id                BIGINT       NOT NULL REFERENCES roles (id),
    username               VARCHAR(128) UNIQUE,
    email_normalized       VARCHAR(255) NOT NULL UNIQUE,
    password_hash          VARCHAR(255) NOT NULL,
    first_name             VARCHAR(120),
    last_name              VARCHAR(120),
    full_name_cipher       TEXT,
    phone_cipher           TEXT,
    active                 BOOLEAN      NOT NULL DEFAULT TRUE,
    account_locked         BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_attempts  INT          NOT NULL DEFAULT 0,
    last_login_at          TIMESTAMPTZ,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX IF NOT EXISTS idx_users_branch ON users (branch_id);
CREATE INDEX IF NOT EXISTS idx_users_role ON users (role_id);

CREATE TEMP TABLE tmp_employee_user_map (
    employee_id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL
) ON COMMIT DROP;

CREATE TEMP TABLE tmp_customer_user_map (
    customer_id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL
) ON COMMIT DROP;

INSERT INTO users (
    branch_id, role_id, username, email_normalized, password_hash, first_name, last_name,
    active, account_locked, failed_login_attempts, last_login_at, created_at, updated_at
)
SELECT
    e.branch_id, e.role_id, e.username, LOWER(TRIM(e.work_email)), e.password_hash, e.first_name, e.last_name,
    e.active, e.account_locked, e.failed_login_attempts, e.last_login_at, e.created_at, e.updated_at
FROM employees e
ON CONFLICT (email_normalized) DO NOTHING;

INSERT INTO tmp_employee_user_map (employee_id, user_id)
SELECT e.id, u.id
FROM employees e
JOIN users u ON u.email_normalized = LOWER(TRIM(e.work_email));

INSERT INTO users (
    branch_id, role_id, username, email_normalized, password_hash, first_name, last_name, full_name_cipher, phone_cipher,
    active, account_locked, failed_login_attempts, last_login_at, created_at, updated_at
)
SELECT
    c.branch_id,
    r.id,
    'legacy_customer_' || c.id::text,
    'legacy-customer-' || c.id::text || '@local.invalid',
    'NO_LOGIN_BOOKING_ONLY',
    NULL,
    NULL,
    c.full_name_cipher,
    c.phone_cipher,
    TRUE,
    FALSE,
    0,
    NULL,
    c.created_at,
    c.updated_at
FROM customers c
JOIN roles r ON r.name = 'ROLE_CUSTOMER'
ON CONFLICT (email_normalized) DO NOTHING;

INSERT INTO tmp_customer_user_map (customer_id, user_id)
SELECT c.id, u.id
FROM customers c
JOIN users u ON u.username = 'legacy_customer_' || c.id::text;

UPDATE tmp_customer_user_map m
SET user_id = u.id
FROM customer_accounts ca
JOIN users u ON u.email_normalized = ca.email_normalized
WHERE ca.customer_id = m.customer_id;

UPDATE users u
SET
    email_normalized = ca.email_normalized,
    password_hash = ca.password_hash,
    role_id = ca.role_id,
    active = ca.active
FROM customer_accounts ca
JOIN tmp_customer_user_map m ON m.customer_id = ca.customer_id
WHERE u.id = m.user_id
  AND u.username LIKE 'legacy_customer_%';

ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_customer_id_fkey;
ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_employee_id_fkey;
ALTER TABLE employee_services DROP CONSTRAINT IF EXISTS employee_services_employee_id_fkey;
ALTER TABLE server_side_sessions DROP CONSTRAINT IF EXISTS server_side_sessions_employee_id_fkey;
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_performed_by_employee_id_fkey;
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS fk_audit_logs_actor_employee;

UPDATE appointments a
SET customer_id = m.user_id
FROM tmp_customer_user_map m
WHERE a.customer_id = m.customer_id;

UPDATE appointments a
SET employee_id = m.user_id
FROM tmp_employee_user_map m
WHERE a.employee_id = m.employee_id;

UPDATE employee_services es
SET employee_id = m.user_id
FROM tmp_employee_user_map m
WHERE es.employee_id = m.employee_id;

ALTER TABLE server_side_sessions RENAME COLUMN employee_id TO user_id;
UPDATE server_side_sessions s
SET user_id = m.user_id
FROM tmp_employee_user_map m
WHERE s.user_id = m.employee_id;

ALTER TABLE audit_logs RENAME COLUMN performed_by_employee_id TO performed_by_user_id;
UPDATE audit_logs a
SET performed_by_user_id = m.user_id
FROM tmp_employee_user_map m
WHERE a.performed_by_user_id = m.employee_id;

UPDATE audit_logs a
SET actor_employee_id = m.user_id
FROM tmp_employee_user_map m
WHERE a.actor_employee_id = m.employee_id;

ALTER TABLE appointments
    ADD CONSTRAINT appointments_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES users (id),
    ADD CONSTRAINT appointments_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES users (id);

ALTER TABLE employee_services
    ADD CONSTRAINT employee_services_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE server_side_sessions
    ADD CONSTRAINT server_side_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE audit_logs
    ADD CONSTRAINT audit_logs_performed_by_user_id_fkey FOREIGN KEY (performed_by_user_id) REFERENCES users (id);

ALTER TABLE audit_logs
    ADD CONSTRAINT fk_audit_logs_actor_employee FOREIGN KEY (actor_employee_id) REFERENCES users (id);

DROP TABLE IF EXISTS customer_accounts;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS employees;
