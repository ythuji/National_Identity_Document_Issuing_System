-- ============================================================================
-- NIDIS Seed Data (Spring Boot data.sql for Microsoft SQL Server)
-- Runs automatically on application startup (IF NOT EXISTS guards)
-- ============================================================================

-- 1. Seed default roles
IF NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'APPLICANT')
    INSERT INTO roles (role_name) VALUES ('APPLICANT')
GO

IF NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'VERIFICATION_OFFICER')
    INSERT INTO roles (role_name) VALUES ('VERIFICATION_OFFICER')
GO

IF NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'SUPER_ADMIN')
    INSERT INTO roles (role_name) VALUES ('SUPER_ADMIN')
GO

-- Default Password for all seed accounts: Password123
-- BCrypt: $2a$10$qWNTmABu3DexmEsZ6C1eOOFreHvF9Kqp5hyvIOP9WA/f2CUT3HckW

-- 2. Seed Super Admin account: admin@nidis.gov.lk
IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@nidis.gov.lk')
    INSERT INTO users (full_name, email, nic_number, password_hash, phone, is_active, is_email_verified, created_at, updated_at)
    VALUES ('System Administrator', 'admin@nidis.gov.lk', '198000000001',
            '$2a$10$qWNTmABu3DexmEsZ6C1eOOFreHvF9Kqp5hyvIOP9WA/f2CUT3HckW',
            '0710000001', 1, 1, GETDATE(), GETDATE())
GO

IF NOT EXISTS (
    SELECT 1 FROM user_roles ur
    JOIN users u ON ur.user_id = u.id
    JOIN roles r ON ur.role_id = r.id
    WHERE u.email = 'admin@nidis.gov.lk' AND r.role_name = 'SUPER_ADMIN'
)
    INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u, roles r
    WHERE u.email = 'admin@nidis.gov.lk' AND r.role_name = 'SUPER_ADMIN'
GO

-- 3. Seed Verification Officer account: officer@nidis.gov.lk
IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'officer@nidis.gov.lk')
    INSERT INTO users (full_name, email, nic_number, password_hash, phone, is_active, is_email_verified, created_at, updated_at)
    VALUES ('Officer Kamal Perera', 'officer@nidis.gov.lk', '198500000002',
            '$2a$10$qWNTmABu3DexmEsZ6C1eOOFreHvF9Kqp5hyvIOP9WA/f2CUT3HckW',
            '0770000002', 1, 1, GETDATE(), GETDATE())
GO

IF NOT EXISTS (
    SELECT 1 FROM user_roles ur
    JOIN users u ON ur.user_id = u.id
    JOIN roles r ON ur.role_id = r.id
    WHERE u.email = 'officer@nidis.gov.lk' AND r.role_name = 'VERIFICATION_OFFICER'
)
    INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u, roles r
    WHERE u.email = 'officer@nidis.gov.lk' AND r.role_name = 'VERIFICATION_OFFICER'
GO

-- 4. Seed Citizen / Applicant account: citizen@test.lk
IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'citizen@test.lk')
    INSERT INTO users (full_name, email, nic_number, password_hash, phone, is_active, is_email_verified, created_at, updated_at)
    VALUES ('Citizen Test User', 'citizen@test.lk', '200012345678',
            '$2a$10$qWNTmABu3DexmEsZ6C1eOOFreHvF9Kqp5hyvIOP9WA/f2CUT3HckW',
            '0771234567', 1, 1, GETDATE(), GETDATE())
GO

IF NOT EXISTS (
    SELECT 1 FROM user_roles ur
    JOIN users u ON ur.user_id = u.id
    JOIN roles r ON ur.role_id = r.id
    WHERE u.email = 'citizen@test.lk' AND r.role_name = 'APPLICANT'
)
    INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u, roles r
    WHERE u.email = 'citizen@test.lk' AND r.role_name = 'APPLICANT'
GO

-- 5. Seed Secondary Citizen: kamal@test.lk
IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'kamal@test.lk')
    INSERT INTO users (full_name, email, nic_number, password_hash, phone, is_active, is_email_verified, created_at, updated_at)
    VALUES ('Kamal Silva', 'kamal@test.lk', '199515004128',
            '$2a$10$qWNTmABu3DexmEsZ6C1eOOFreHvF9Kqp5hyvIOP9WA/f2CUT3HckW',
            '0719876543', 1, 1, GETDATE(), GETDATE())
GO

IF NOT EXISTS (
    SELECT 1 FROM user_roles ur
    JOIN users u ON ur.user_id = u.id
    JOIN roles r ON ur.role_id = r.id
    WHERE u.email = 'kamal@test.lk' AND r.role_name = 'APPLICANT'
)
    INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u, roles r
    WHERE u.email = 'kamal@test.lk' AND r.role_name = 'APPLICANT'
GO

-- 6. Seed Sample Applications with Official Sri Lankan Numbers

-- Sample 1: Shipped Smart NIC with issued 12-digit number (200012345678)
IF NOT EXISTS (SELECT 1 FROM nic_applications WHERE reference_number = 'NIC-2026-DEMO01')
BEGIN
    DECLARE @CitId1 BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'citizen@test.lk');
    INSERT INTO nic_applications (
        user_id, category, reference_number, existing_nic_number, issued_nic_number,
        full_name, date_of_birth, gender, civil_status, occupation, permanent_address,
        phone, grama_niladhari_division, divisional_secretariat, status, is_draft,
        payment_status, fee_amount, officer_comment, created_at, updated_at
    ) VALUES (
        @CitId1, 'NEW', 'NIC-2026-DEMO01', NULL, '200012345678',
        'Citizen Test User', '2000-05-02', 'Male', 'Single', 'Software Engineer',
        'No. 45/2, Galle Road, Colombo 03', '0771234567', 'Kollupitiya North 514A',
        'Colombo', 'SHIPPED', 0, 'PAID', 500.0,
        'Biometrics and birth certificate verified. Smart NIC issued and dispatched.',
        DATEADD(DAY, -5, GETDATE()), GETDATE()
    );
END
GO

-- Sample 2: NIC Under Review (appears in Officer Review Queue)
IF NOT EXISTS (SELECT 1 FROM nic_applications WHERE reference_number = 'NIC-2026-DEMO02')
BEGIN
    DECLARE @KamalId1 BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'kamal@test.lk');
    INSERT INTO nic_applications (
        user_id, category, reference_number, existing_nic_number, issued_nic_number,
        full_name, date_of_birth, gender, civil_status, occupation, permanent_address,
        phone, grama_niladhari_division, divisional_secretariat, status, is_draft,
        payment_status, fee_amount, officer_comment, created_at, updated_at
    ) VALUES (
        @KamalId1, 'RENEWAL', 'NIC-2026-DEMO02', '199515004128', NULL,
        'Kamal Silva', '1995-05-30', 'Male', 'Married', 'Accountant',
        'No. 12, Kandy Road, Kiribathgoda', '0719876543', 'Kiribathgoda West',
        'Kelaniya', 'UNDER_REVIEW', 0, 'PAID', 500.0,
        'Application submitted and paid. Awaiting verification review.',
        DATEADD(DAY, -1, GETDATE()), GETDATE()
    );
END
GO

-- Sample 3: Shipped Driving License with issued DMT number (B8294105)
IF NOT EXISTS (SELECT 1 FROM license_applications WHERE reference_number = 'LIC-2026-DEMO01')
BEGIN
    DECLARE @CitId2 BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'citizen@test.lk');
    INSERT INTO license_applications (
        user_id, category, reference_number, existing_license_number, issued_license_number,
        full_name, date_of_birth, address, phone, vehicle_class, medical_cert_required,
        status, is_draft, officer_comment, created_at, updated_at
    ) VALUES (
        @CitId2, 'NEW', 'LIC-2026-DEMO01', NULL, 'B8294105',
        'Citizen Test User', '2000-05-02', 'No. 45/2, Galle Road, Colombo 03', '0771234567',
        'B, B1 (Light Vehicles & Auto)', 1, 'SHIPPED', 0,
        'Practical and medical certified. License card dispatched.',
        DATEADD(DAY, -4, GETDATE()), GETDATE()
    );
END
GO

-- Sample 4: Driving License APPROVED & PAID (Ready for Officer "Verify Payment & Dispatch" testing)
IF NOT EXISTS (SELECT 1 FROM license_applications WHERE reference_number = 'LIC-2026-DEMO02')
BEGIN
    DECLARE @KamalId2 BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'kamal@test.lk');
    INSERT INTO license_applications (
        user_id, category, reference_number, existing_license_number, issued_license_number,
        full_name, date_of_birth, address, phone, vehicle_class, medical_cert_required,
        status, is_draft, officer_comment, created_at, updated_at
    ) VALUES (
        @KamalId2, 'NEW', 'LIC-2026-DEMO02', NULL, NULL,
        'Kamal Silva', '1995-05-30', 'No. 12, Kandy Road, Kiribathgoda', '0719876543',
        'A, B (Motorcycle & Car)', 1, 'APPROVED', 0,
        'Officer verified bio-data and NTMI medical certificate. Approved for payment & dispatch.',
        DATEADD(DAY, -2, GETDATE()), GETDATE()
    );
END
GO

-- Sample 5: Shipped Passport with issued DIE number (N7482910)
IF NOT EXISTS (SELECT 1 FROM passport_applications WHERE reference_number = 'PASS-2026-DEMO01')
BEGIN
    DECLARE @CitId3 BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'citizen@test.lk');
    INSERT INTO passport_applications (
        user_id, category, reference_number, existing_passport_number, issued_passport_number,
        full_name, date_of_birth, place_of_birth, nationality, gender, address, phone,
        profession, dual_citizenship, status, is_draft, officer_comment, created_at, updated_at
    ) VALUES (
        @CitId3, 'NEW', 'PASS-2026-DEMO01', NULL, 'N7482910',
        'Citizen Test User', '2000-05-02', 'Colombo', 'Sri Lankan', 'Male',
        'No. 45/2, Galle Road, Colombo 03', '0771234567', 'Software Engineer', 0,
        'SHIPPED', 0, 'ICAO biometric passport approved, printed and shipped via postal courier.',
        DATEADD(DAY, -3, GETDATE()), GETDATE()
    );
END
GO

-- Sample Payments
IF NOT EXISTS (SELECT 1 FROM payment_transactions WHERE transaction_id = 'TXN-DEMO-NIC01')
BEGIN
    DECLARE @CitPay1 BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'citizen@test.lk');
    DECLARE @NicId1 BIGINT = (SELECT TOP 1 id FROM nic_applications WHERE reference_number = 'NIC-2026-DEMO01');
    INSERT INTO payment_transactions (
        transaction_id, user_id, application_type, application_id, application_reference,
        amount, payment_method, card_last_four, payment_status, paid_at, created_at
    ) VALUES (
        'TXN-DEMO-NIC01', @CitPay1, 'NIC', @NicId1, 'NIC-2026-DEMO01',
        500.0, 'VISA/MASTERCARD', '4242', 'PAID', DATEADD(DAY, -5, GETDATE()), DATEADD(DAY, -5, GETDATE())
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM payment_transactions WHERE transaction_id = 'TXN-DEMO-LIC01')
BEGIN
    DECLARE @CitPay2 BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'citizen@test.lk');
    DECLARE @LicId1 BIGINT = (SELECT TOP 1 id FROM license_applications WHERE reference_number = 'LIC-2026-DEMO01');
    INSERT INTO payment_transactions (
        transaction_id, user_id, application_type, application_id, application_reference,
        amount, payment_method, card_last_four, payment_status, paid_at, created_at
    ) VALUES (
        'TXN-DEMO-LIC01', @CitPay2, 'LICENSE', @LicId1, 'LIC-2026-DEMO01',
        2500.0, 'VISA/MASTERCARD', '4242', 'PAID', DATEADD(DAY, -4, GETDATE()), DATEADD(DAY, -4, GETDATE())
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM payment_transactions WHERE transaction_id = 'TXN-DEMO-LIC02')
BEGIN
    DECLARE @KamalPay1 BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'kamal@test.lk');
    DECLARE @LicId2 BIGINT = (SELECT TOP 1 id FROM license_applications WHERE reference_number = 'LIC-2026-DEMO02');
    INSERT INTO payment_transactions (
        transaction_id, user_id, application_type, application_id, application_reference,
        amount, payment_method, card_last_four, payment_status, paid_at, created_at
    ) VALUES (
        'TXN-DEMO-LIC02', @KamalPay1, 'LICENSE', @LicId2, 'LIC-2026-DEMO02',
        2500.0, 'VISA/MASTERCARD', '1111', 'PAID', DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -1, GETDATE())
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM payment_transactions WHERE transaction_id = 'TXN-DEMO-PASS01')
BEGIN
    DECLARE @CitPay3 BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'citizen@test.lk');
    DECLARE @PassId1 BIGINT = (SELECT TOP 1 id FROM passport_applications WHERE reference_number = 'PASS-2026-DEMO01');
    INSERT INTO payment_transactions (
        transaction_id, user_id, application_type, application_id, application_reference,
        amount, payment_method, card_last_four, payment_status, paid_at, created_at
    ) VALUES (
        'TXN-DEMO-PASS01', @CitPay3, 'PASSPORT', @PassId1, 'PASS-2026-DEMO01',
        5000.0, 'VISA/MASTERCARD', '4242', 'PAID', DATEADD(DAY, -3, GETDATE()), DATEADD(DAY, -3, GETDATE())
    );
END
GO
