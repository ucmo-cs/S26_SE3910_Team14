-- Reset legacy occupancy marks so availability is driven by new bookings/reschedules
-- under the slot inventory model.
UPDATE appointment_slot_inventory
SET appointment_id = NULL,
    updated_at = (NOW() AT TIME ZONE 'UTC');
