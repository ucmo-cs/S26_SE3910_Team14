CREATE TABLE IF NOT EXISTS guests (
    id             BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT       NOT NULL UNIQUE REFERENCES appointments (id) ON DELETE CASCADE,
    first_name     VARCHAR(120) NOT NULL,
    last_name      VARCHAR(120) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX IF NOT EXISTS idx_guests_appointment_id ON guests (appointment_id);
