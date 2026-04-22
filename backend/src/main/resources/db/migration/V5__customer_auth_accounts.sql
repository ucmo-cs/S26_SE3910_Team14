CREATE TABLE IF NOT EXISTS customer_accounts (
    id               BIGSERIAL PRIMARY KEY,
    customer_id      BIGINT       NOT NULL UNIQUE REFERENCES customers (id) ON DELETE CASCADE,
    email_normalized VARCHAR(255) NOT NULL UNIQUE,
    password_hash    VARCHAR(255) NOT NULL,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX IF NOT EXISTS idx_appointments_customer_time
    ON appointments (customer_id, scheduled_start);
