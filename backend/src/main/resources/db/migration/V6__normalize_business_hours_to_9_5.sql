UPDATE branch_business_hours
SET open_time = TIME '09:00',
    close_time = TIME '17:00',
    active = TRUE
WHERE active = TRUE;
