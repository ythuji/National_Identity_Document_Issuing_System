-- Seed / Update default roles
IF NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'APPLICANT')
    INSERT INTO roles (role_name) VALUES ('APPLICANT');
IF NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'VERIFICATION_OFFICER')
    INSERT INTO roles (role_name) VALUES ('VERIFICATION_OFFICER');
IF NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'SUPER_ADMIN')
    INSERT INTO roles (role_name) VALUES ('SUPER_ADMIN');

-- Consistent BCrypt hash for 'Password123'
DECLARE @pwd VARCHAR(255) = '$2a$10$qWNTmABu3DexmEsZ6C1eOOFreHvF9Kqp5hyvIOP9WA/f2CUT3HckW';

-- 1. Super Admin Account: admin@nidis.gov.lk
IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@nidis.gov.lk')
    INSERT INTO users (full_name, email, nic_number, password_hash, phone, is_active, is_email_verified, created_at, updated_at)
    VALUES ('System Administrator', 'admin@nidis.gov.lk', '198000000001', @pwd, '0710000001', 1, 1, GETDATE(), GETDATE());
ELSE
    UPDATE users SET password_hash = @pwd, is_active = 1, is_email_verified = 1 WHERE email = 'admin@nidis.gov.lk';

-- Assign SUPER_ADMIN role
IF NOT EXISTS (
    SELECT 1 FROM user_roles ur
    JOIN users u ON ur.user_id = u.id
    JOIN roles r ON ur.role_id = r.id
    WHERE u.email = 'admin@nidis.gov.lk' AND r.role_name = 'SUPER_ADMIN'
)
    INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u, roles r
    WHERE u.email = 'admin@nidis.gov.lk' AND r.role_name = 'SUPER_ADMIN';

-- 2. Verification Officer Account: officer@nidis.gov.lk
IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'officer@nidis.gov.lk')
    INSERT INTO users (full_name, email, nic_number, password_hash, phone, is_active, is_email_verified, created_at, updated_at)
    VALUES ('Officer Kamal Perera', 'officer@nidis.gov.lk', '198500000002', @pwd, '0770000002', 1, 1, GETDATE(), GETDATE());
ELSE
    UPDATE users SET password_hash = @pwd, is_active = 1, is_email_verified = 1 WHERE email = 'officer@nidis.gov.lk';

-- Assign VERIFICATION_OFFICER role
IF NOT EXISTS (
    SELECT 1 FROM user_roles ur
    JOIN users u ON ur.user_id = u.id
    JOIN roles r ON ur.role_id = r.id
    WHERE u.email = 'officer@nidis.gov.lk' AND r.role_name = 'VERIFICATION_OFFICER'
)
    INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u, roles r
    WHERE u.email = 'officer@nidis.gov.lk' AND r.role_name = 'VERIFICATION_OFFICER';

-- 3. Citizen / Applicant Account: citizen@test.lk
IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'citizen@test.lk')
    INSERT INTO users (full_name, email, nic_number, password_hash, phone, is_active, is_email_verified, created_at, updated_at)
    VALUES ('Citizen Test User', 'citizen@test.lk', '200012345678', @pwd, '0771234567', 1, 1, GETDATE(), GETDATE());
ELSE
    UPDATE users SET password_hash = @pwd, is_active = 1, is_email_verified = 1 WHERE email = 'citizen@test.lk';

-- Assign APPLICANT role
IF NOT EXISTS (
    SELECT 1 FROM user_roles ur
    JOIN users u ON ur.user_id = u.id
    JOIN roles r ON ur.role_id = r.id
    WHERE u.email = 'citizen@test.lk' AND r.role_name = 'APPLICANT'
)
    INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u, roles r
    WHERE u.email = 'citizen@test.lk' AND r.role_name = 'APPLICANT';

