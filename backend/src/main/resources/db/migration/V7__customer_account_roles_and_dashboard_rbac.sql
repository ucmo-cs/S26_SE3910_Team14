SELECT setval(
    pg_get_serial_sequence('roles', 'id'),
    COALESCE((SELECT MAX(id) FROM roles), 0),
    true
);

INSERT INTO roles (name, description)
SELECT 'ROLE_CUSTOMER', 'Customer portal access'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_CUSTOMER');

INSERT INTO roles (name, description)
SELECT 'ROLE_EMPLOYEE', 'Employee operational dashboard access'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_EMPLOYEE');

INSERT INTO roles (name, description)
SELECT 'ROLE_ADMIN', 'Administrative full-access dashboard role'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_ADMIN');

ALTER TABLE customer_accounts
    ADD COLUMN IF NOT EXISTS role_id BIGINT;

UPDATE customer_accounts ca
SET role_id = r.id
FROM roles r
WHERE ca.role_id IS NULL
  AND r.name = 'ROLE_CUSTOMER';

ALTER TABLE customer_accounts
    ALTER COLUMN role_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_name = 'customer_accounts'
          AND constraint_name = 'fk_customer_accounts_role'
    ) THEN
        ALTER TABLE customer_accounts
            ADD CONSTRAINT fk_customer_accounts_role
                FOREIGN KEY (role_id) REFERENCES roles (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_customer_accounts_role ON customer_accounts (role_id);
