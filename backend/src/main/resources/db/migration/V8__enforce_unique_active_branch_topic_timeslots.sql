WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY branch_id, service_type_id, scheduled_start
               ORDER BY id
           ) AS rn
    FROM appointments
    WHERE status <> 'CANCELLED'
)
UPDATE appointments
SET status = 'CANCELLED',
    updated_at = (NOW() AT TIME ZONE 'UTC')
WHERE id IN (
    SELECT id FROM ranked WHERE rn > 1
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_appointments_active_branch_topic_start
    ON appointments (branch_id, service_type_id, scheduled_start)
    WHERE status <> 'CANCELLED';
