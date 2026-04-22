CREATE TABLE appointment_slot_inventory (
    id               BIGSERIAL PRIMARY KEY,
    branch_id        BIGINT      NOT NULL REFERENCES branches (id) ON DELETE CASCADE,
    service_type_id  BIGINT      NOT NULL REFERENCES service_types (id) ON DELETE CASCADE,
    slot_date        DATE        NOT NULL,
    slot_start_time  TIME        NOT NULL,
    appointment_id   BIGINT      REFERENCES appointments (id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_slot_inventory UNIQUE (branch_id, service_type_id, slot_date, slot_start_time)
);

CREATE INDEX idx_slot_inventory_lookup
    ON appointment_slot_inventory (branch_id, service_type_id, slot_date, slot_start_time);

CREATE INDEX idx_slot_inventory_appointment
    ON appointment_slot_inventory (appointment_id)
    WHERE appointment_id IS NOT NULL;

-- Backfill slot rows for existing appointment dates for each branch/topic pair.
INSERT INTO appointment_slot_inventory (branch_id, service_type_id, slot_date, slot_start_time, appointment_id)
SELECT seed.branch_id,
       seed.service_type_id,
       seed.slot_date,
       (TIME '09:00' + (slot.n * INTERVAL '30 minutes'))::time,
       NULL
FROM (
    SELECT DISTINCT a.branch_id,
                    a.service_type_id,
                    ((a.scheduled_start AT TIME ZONE COALESCE(b.time_zone, 'America/Chicago'))::date) AS slot_date
    FROM appointments a
    JOIN branches b ON b.id = a.branch_id
) AS seed
         CROSS JOIN generate_series(0, 15) AS slot(n)
ON CONFLICT (branch_id, service_type_id, slot_date, slot_start_time) DO NOTHING;

-- Mark occupied slots for non-cancelled existing appointments.
UPDATE appointment_slot_inventory inv
SET appointment_id = a.id,
    updated_at = (NOW() AT TIME ZONE 'UTC')
FROM appointments a
         JOIN branches b ON b.id = a.branch_id
WHERE a.status <> 'CANCELLED'
  AND inv.branch_id = a.branch_id
  AND inv.service_type_id = a.service_type_id
  AND inv.slot_date = ((a.scheduled_start AT TIME ZONE COALESCE(b.time_zone, 'America/Chicago'))::date)
  AND inv.slot_start_time >= ((a.scheduled_start AT TIME ZONE COALESCE(b.time_zone, 'America/Chicago'))::time)
  AND inv.slot_start_time < ((a.scheduled_end AT TIME ZONE COALESCE(b.time_zone, 'America/Chicago'))::time);
