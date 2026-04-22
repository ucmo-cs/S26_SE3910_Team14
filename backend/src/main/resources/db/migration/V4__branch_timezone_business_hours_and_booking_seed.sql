ALTER TABLE branches
    ADD COLUMN IF NOT EXISTS time_zone VARCHAR(64) NOT NULL DEFAULT 'America/Chicago';

CREATE TABLE IF NOT EXISTS branch_business_hours (
    id          BIGSERIAL PRIMARY KEY,
    branch_id   BIGINT      NOT NULL REFERENCES branches (id) ON DELETE CASCADE,
    day_of_week SMALLINT    NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    open_time   TIME        NOT NULL,
    close_time  TIME        NOT NULL,
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT chk_branch_business_hours_window CHECK (close_time > open_time),
    CONSTRAINT uq_branch_business_hours UNIQUE (branch_id, day_of_week)
);

CREATE INDEX IF NOT EXISTS idx_branch_business_hours_branch_day
    ON branch_business_hours (branch_id, day_of_week);

-- Seed additional branch data for realistic topic filtering.
INSERT INTO branches (id, code, display_name, street_line1, city, state_province, postal_code, country_code, time_zone, active)
VALUES
    (2, 'WEST', 'West Milwaukee Branch', '5600 W National Ave', 'West Milwaukee', 'WI', '53214', 'US', 'America/Chicago', TRUE)
ON CONFLICT (id) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    street_line1 = EXCLUDED.street_line1,
    city = EXCLUDED.city,
    state_province = EXCLUDED.state_province,
    postal_code = EXCLUDED.postal_code,
    country_code = EXCLUDED.country_code,
    time_zone = EXCLUDED.time_zone,
    active = EXCLUDED.active;

UPDATE branches
SET time_zone = 'America/Chicago'
WHERE time_zone IS NULL OR time_zone = '';

-- Seed service topics used by public booking.
INSERT INTO service_types (id, branch_id, code, display_name, description, default_duration_minutes, active)
VALUES
    (1, NULL, 'GENERAL_BANKING', 'General Banking', 'Account maintenance, deposits, and account questions.', 30, TRUE),
    (2, NULL, 'CREDIT_CARDS', 'Credit Cards', 'Apply, service, or review your card options.', 30, TRUE),
    (3, NULL, 'LOANS', 'Loans & Mortgages', 'Discuss personal, auto, and home loan products.', 30, TRUE)
ON CONFLICT (id) DO UPDATE SET
    branch_id = EXCLUDED.branch_id,
    code = EXCLUDED.code,
    display_name = EXCLUDED.display_name,
    description = EXCLUDED.description,
    default_duration_minutes = EXCLUDED.default_duration_minutes,
    active = EXCLUDED.active;

-- Additional employees used to support service coverage at branches.
INSERT INTO employees (id, branch_id, role_id, username, first_name, last_name, work_email, password_hash, active)
VALUES
    (2, 1, 1, 'advisor_main', 'Alex', 'Rivera', 'advisor.main@bank.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HCGFGLn1EK1FPD1H3W8j6', TRUE),
    (3, 2, 1, 'advisor_west', 'Morgan', 'Lee', 'advisor.west@bank.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HCGFGLn1EK1FPD1H3W8j6', TRUE)
ON CONFLICT (id) DO UPDATE SET
    branch_id = EXCLUDED.branch_id,
    role_id = EXCLUDED.role_id,
    username = EXCLUDED.username,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    work_email = EXCLUDED.work_email,
    active = EXCLUDED.active;

INSERT INTO employee_services (employee_id, service_type_id)
VALUES
    (1, 1), (1, 2), (1, 3),
    (2, 1), (2, 2),
    (3, 2), (3, 3)
ON CONFLICT (employee_id, service_type_id) DO NOTHING;

-- Weekday and Saturday business hours per branch.
INSERT INTO branch_business_hours (branch_id, day_of_week, open_time, close_time, active)
SELECT b.id, d.day_of_week, d.open_time, d.close_time, TRUE
FROM branches b
         CROSS JOIN (
    VALUES
        (1, TIME '08:00', TIME '17:00'),
        (2, TIME '08:00', TIME '17:00'),
        (3, TIME '08:00', TIME '17:00'),
        (4, TIME '08:00', TIME '17:00'),
        (5, TIME '08:00', TIME '17:00'),
        (6, TIME '09:00', TIME '13:00')
) AS d(day_of_week, open_time, close_time)
WHERE b.active = TRUE
ON CONFLICT (branch_id, day_of_week) DO UPDATE SET
    open_time = EXCLUDED.open_time,
    close_time = EXCLUDED.close_time,
    active = EXCLUDED.active;
