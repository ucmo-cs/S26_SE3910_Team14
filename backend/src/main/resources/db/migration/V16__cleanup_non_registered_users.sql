-- Keep only seeded/demo identities (1, 2, 3) and fallback public customer (99) in users.
-- Re-point historical references before deleting other users.
INSERT INTO users (
    id,
    branch_id,
    role_id,
    username,
    email_normalized,
    password_hash,
    first_name,
    last_name,
    full_name_cipher,
    active,
    account_locked,
    failed_login_attempts
)
SELECT
    99,
    1,
    r.id,
    'public.booking.fallback',
    'public-booking-fallback@local.invalid',
    'NO_LOGIN_BOOKING_ONLY',
    'Public',
    'Booking',
    'Public Booking',
    TRUE,
    FALSE,
    0
FROM roles r
WHERE r.name = 'ROLE_CUSTOMER'
ON CONFLICT (id) DO NOTHING;

UPDATE appointments
SET customer_id = 99
WHERE customer_id NOT IN (1, 2, 3, 99);

UPDATE appointments
SET employee_id = 2
WHERE employee_id NOT IN (1, 2, 3, 99);

DELETE FROM server_side_sessions
WHERE user_id NOT IN (1, 2, 3, 99);

DELETE FROM employee_services
WHERE employee_id NOT IN (1, 2, 3, 99);

UPDATE audit_logs
SET performed_by_user_id = NULL
WHERE performed_by_user_id NOT IN (1, 2, 3, 99);

UPDATE audit_logs
SET actor_employee_id = NULL
WHERE actor_employee_id NOT IN (1, 2, 3, 99);

DELETE FROM users
WHERE id NOT IN (1, 2, 3, 99);
