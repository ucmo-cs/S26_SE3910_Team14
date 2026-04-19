-- Insert a Branch (Using integer ID 1)
INSERT INTO branches (id, code, display_name)
VALUES (1, 'MAIN', 'Main Street Branch');

-- Insert a Role (Using integer ID 1)
INSERT INTO roles (id, name, description)
VALUES (1, 'ROLE_BRANCH_MANAGER', 'Branch Manager with full branch access');

-- Insert an Employee (Using integer ID 1)
-- The BCrypt hash below equals 'password123'
INSERT INTO employees (id, branch_id, role_id, username, first_name, last_name, work_email, password_hash, active)
VALUES (
    1, 
    1, 
    1, 
    'manager',
    'Test',
    'Manager',
    'manager@bank.com', 
    '$2a$12$CwTycUXWue0Thq9StjUM0u1.amB/jO.2u.D.A.A.A.A.A.A.A.A.A', 
    true
);