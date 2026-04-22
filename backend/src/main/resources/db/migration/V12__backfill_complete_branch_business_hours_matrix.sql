-- Ensure every branch has a business-hours row for each day of week (1..7).
-- This prevents gaps that can cause inconsistent availability behavior.
INSERT INTO branch_business_hours (branch_id, day_of_week, open_time, close_time, active)
SELECT b.id, d.day_of_week, TIME '09:00', TIME '17:00', CASE WHEN d.day_of_week BETWEEN 1 AND 5 THEN TRUE ELSE FALSE END
FROM branches b
CROSS JOIN (
    VALUES (1), (2), (3), (4), (5), (6), (7)
) AS d(day_of_week)
ON CONFLICT (branch_id, day_of_week) DO NOTHING;

-- Normalize all existing rows to the current booking policy:
-- Monday-Friday 9:00-17:00 active, weekends inactive.
UPDATE branch_business_hours
SET open_time = TIME '09:00',
    close_time = TIME '17:00',
    active = CASE WHEN day_of_week BETWEEN 1 AND 5 THEN TRUE ELSE FALSE END;
