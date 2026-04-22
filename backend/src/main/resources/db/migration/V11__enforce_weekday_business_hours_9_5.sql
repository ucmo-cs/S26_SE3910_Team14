-- Normalize branch hours to weekday-only booking windows for public appointments.
UPDATE branch_business_hours
SET open_time = TIME '09:00',
    close_time = TIME '17:00',
    active = CASE WHEN day_of_week BETWEEN 1 AND 5 THEN TRUE ELSE FALSE END;
