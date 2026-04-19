-- V2 used a BCrypt string that does not verify against 'password123' (bad copy/paste or wrong generator).
-- This hash is BCrypt cost 12 for plaintext: password123
UPDATE employees
SET password_hash = '$2b$12$hiukuixF03MCXutZZBe95uSJ4RpsssxLG8GM6K25g21OXS5pbYxvC'
WHERE username = 'manager';
