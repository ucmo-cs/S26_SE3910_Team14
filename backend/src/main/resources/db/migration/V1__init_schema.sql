-- RBAC scheduling core schema (PostgreSQL)
-- CMMC-oriented: explicit FKs, indexes for audit queries, UTC timestamps

CREATE TABLE branches (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(32)  NOT NULL UNIQUE,
    display_name    VARCHAR(160) NOT NULL,
    street_line1    VARCHAR(255),
    street_line2    VARCHAR(255),
    city            VARCHAR(120),
    state_province  VARCHAR(64),
    postal_code     VARCHAR(32),
    country_code    CHAR(2),
    phone_e164      VARCHAR(32),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE TABLE roles (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(64)  NOT NULL UNIQUE,
    description  VARCHAR(255),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE TABLE employees (
    id                     BIGSERIAL PRIMARY KEY,
    branch_id              BIGINT       NOT NULL REFERENCES branches (id),
    role_id                BIGINT       NOT NULL REFERENCES roles (id),
    username               VARCHAR(128) NOT NULL UNIQUE,
    password_hash          VARCHAR(255) NOT NULL,
    first_name             VARCHAR(120) NOT NULL,
    last_name              VARCHAR(120) NOT NULL,
    work_email             VARCHAR(255) NOT NULL,
    active                 BOOLEAN      NOT NULL DEFAULT TRUE,
    account_locked         BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_attempts  INT          NOT NULL DEFAULT 0,
    last_login_at          TIMESTAMPTZ,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX idx_employees_branch ON employees (branch_id);
CREATE INDEX idx_employees_role ON employees (role_id);

-- Customer PII stored as ciphertext at rest; plaintext exists only in application memory
CREATE TABLE customers (
    id                   BIGSERIAL PRIMARY KEY,
    branch_id            BIGINT REFERENCES branches (id),
    external_reference   VARCHAR(64),
    full_name_cipher     TEXT         NOT NULL,
    email_cipher         TEXT         NOT NULL,
    phone_cipher         TEXT,
    address_line1_cipher TEXT,
    address_line2_cipher TEXT,
    city_cipher          TEXT,
    state_province_cipher TEXT,
    postal_code_cipher   TEXT,
    country_code         CHAR(2),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX idx_customers_branch ON customers (branch_id);
CREATE INDEX idx_customers_external_ref ON customers (external_reference) WHERE external_reference IS NOT NULL;

CREATE TABLE service_types (
    id                        BIGSERIAL PRIMARY KEY,
    branch_id                 BIGINT REFERENCES branches (id),
    code                      VARCHAR(64)  NOT NULL,
    display_name              VARCHAR(160) NOT NULL,
    description               VARCHAR(512),
    default_duration_minutes  INT          NOT NULL CHECK (default_duration_minutes > 0),
    active                    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE UNIQUE INDEX uq_service_types_global_code ON service_types (code) WHERE branch_id IS NULL;
CREATE UNIQUE INDEX uq_service_types_per_branch_code ON service_types (branch_id, code) WHERE branch_id IS NOT NULL;

CREATE INDEX idx_service_types_branch ON service_types (branch_id);

CREATE TABLE employee_services (
    employee_id     BIGINT NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    service_type_id BIGINT NOT NULL REFERENCES service_types (id) ON DELETE CASCADE,
    PRIMARY KEY (employee_id, service_type_id)
);

CREATE TABLE appointments (
    id                       BIGSERIAL PRIMARY KEY,
    branch_id                BIGINT       NOT NULL REFERENCES branches (id),
    customer_id              BIGINT       NOT NULL REFERENCES customers (id),
    employee_id              BIGINT       NOT NULL REFERENCES employees (id),
    service_type_id          BIGINT       NOT NULL REFERENCES service_types (id),
    scheduled_start          TIMESTAMPTZ  NOT NULL,
    scheduled_end            TIMESTAMPTZ  NOT NULL,
    status                   VARCHAR(32)  NOT NULL,
    notes                    VARCHAR(2000),
    optimistic_lock_version  INT          NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT chk_appointments_time_order CHECK (scheduled_end > scheduled_start)
);

CREATE INDEX idx_appointments_branch_time ON appointments (branch_id, scheduled_start);
CREATE INDEX idx_appointments_employee_time ON appointments (employee_id, scheduled_start);
CREATE INDEX idx_appointments_customer ON appointments (customer_id);

-- Long-lived refresh tokens tracked server-side for revocation and session hygiene
CREATE TABLE server_side_sessions (
    id                  BIGSERIAL PRIMARY KEY,
    employee_id         BIGINT       NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    refresh_token_hash  VARCHAR(128) NOT NULL,
    ip_address          VARCHAR(64)  NOT NULL,
    user_agent          VARCHAR(512),
    revoked_at          TIMESTAMPTZ,
    expires_at          TIMESTAMPTZ  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    last_seen_at        TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE UNIQUE INDEX uq_server_side_sessions_active_hash
    ON server_side_sessions (refresh_token_hash)
    WHERE revoked_at IS NULL;

CREATE INDEX idx_server_side_sessions_employee ON server_side_sessions (employee_id);

-- Entity-level audit trail (append-only)
CREATE TABLE audit_logs (
    id                      BIGSERIAL PRIMARY KEY,
    entity_type             VARCHAR(128) NOT NULL,
    entity_id               VARCHAR(64)  NOT NULL,
    action                  VARCHAR(32)  NOT NULL,
    old_state               JSONB,
    new_state               JSONB,
    performed_by_employee_id BIGINT REFERENCES employees (id),
    ip_address              VARCHAR(64),
    correlation_id          VARCHAR(64),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_created ON audit_logs (created_at DESC);
