-- Repair fallback user 99 if plaintext was written into encrypted column.
UPDATE users
SET full_name_cipher = NULL
WHERE id = 99;
