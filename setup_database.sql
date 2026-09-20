-- ============================================================================
-- National Identity Document Issuing System (NIDIS)
-- Database Creation, Schema Setup & Seed Data Script
-- Microsoft SQL Server (T-SQL)
-- ============================================================================

-- STEP 1: CREATE DATABASE IF NOT EXISTS
IF NOT EXISTS (SELECT 1 FROM sys.databases WHERE name = N'NIDIS_DB')
BEGIN
    PRINT 'Creating database NIDIS_DB...';
    CREATE DATABASE [NIDIS_DB];
END
ELSE
BEGIN
    PRINT 'Database NIDIS_DB already exists.';
END
GO

-- STEP 2: CREATE SQL SERVER LOGIN & DATABASE USER IF NOT EXISTS
USE [master];
GO

IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'nidis_admin')
BEGIN
    PRINT 'Creating server login nidis_admin...';
    CREATE LOGIN [nidis_admin] WITH PASSWORD = N'Nidis@2026Secure', CHECK_POLICY = OFF;
END
GO

USE [NIDIS_DB];
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'nidis_admin')
BEGIN
    PRINT 'Creating database user nidis_admin in NIDIS_DB...';
    CREATE USER [nidis_admin] FOR LOGIN [nidis_admin];
    ALTER ROLE [db_owner] ADD MEMBER [nidis_admin];
END
GO

-- ============================================================================
-- STEP 3: CREATE TABLES IF NOT EXISTS
-- ============================================================================

-- 1. Roles Table
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = N'roles')
BEGIN
    CREATE TABLE [dbo].[roles] (
        [id] BIGINT IDENTITY(1,1) NOT NULL,
        [role_name] VARCHAR(50) NOT NULL UNIQUE,
        CONSTRAINT [PK_roles] PRIMARY KEY CLUSTERED ([id] ASC)
    );
    PRINT 'Created table: roles';
END
GO

-- 2. Users Table
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = N'users')
BEGIN
    CREATE TABLE [dbo].[users] (
        [id] BIGINT IDENTITY(1,1) NOT NULL,
        [full_name] VARCHAR(100) NOT NULL,
        [email] VARCHAR(150) NOT NULL UNIQUE,
        [nic_number] VARCHAR(20) NULL,
        [password_hash] VARCHAR(255) NOT NULL,
        [phone] VARCHAR(20) NULL,
        [is_active] BIT NOT NULL CONSTRAINT [DF_users_is_active] DEFAULT (1),
        [is_email_verified] BIT NOT NULL CONSTRAINT [DF_users_is_email_verified] DEFAULT (0),
        [created_at] DATETIME2(7) NOT NULL CONSTRAINT [DF_users_created_at] DEFAULT (GETDATE()),
        [updated_at] DATETIME2(7) NOT NULL CONSTRAINT [DF_users_updated_at] DEFAULT (GETDATE()),
        CONSTRAINT [PK_users] PRIMARY KEY CLUSTERED ([id] ASC)
    );
    PRINT 'Created table: users';
END
GO

-- 3. User Roles Mapping Table
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = N'user_roles')
BEGIN
    CREATE TABLE [dbo].[user_roles] (
        [user_id] BIGINT NOT NULL,
        [role_id] BIGINT NOT NULL,
        CONSTRAINT [PK_user_roles] PRIMARY KEY CLUSTERED ([user_id] ASC, [role_id] ASC),
        CONSTRAINT [FK_user_roles_users] FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([id]) ON DELETE CASCADE,
        CONSTRAINT [FK_user_roles_roles] FOREIGN KEY ([role_id]) REFERENCES [dbo].[roles] ([id]) ON DELETE CASCADE
    );
    PRINT 'Created table: user_roles';
END
GO

-- 4. NIC Applications Table
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = N'nic_applications')
BEGIN
    CREATE TABLE [dbo].[nic_applications] (
        [id] BIGINT IDENTITY(1,1) NOT NULL,
        [user_id] BIGINT NOT NULL,
        [category] VARCHAR(20) NOT NULL,
        [reference_number] VARCHAR(30) NOT NULL UNIQUE,
        [existing_nic_number] VARCHAR(20) NULL,
        [issued_nic_number] VARCHAR(20) NULL,
        [full_name] VARCHAR(100) NOT NULL,
        [date_of_birth] DATE NOT NULL,
        [gender] VARCHAR(20) NOT NULL,
        [civil_status] VARCHAR(30) NOT NULL,
        [occupation] VARCHAR(100) NULL,
        [permanent_address] VARCHAR(255) NOT NULL,
        [phone] VARCHAR(20) NOT NULL,
        [grama_niladhari_division] VARCHAR(100) NOT NULL,
        [divisional_secretariat] VARCHAR(100) NOT NULL,
        [police_report_ref] VARCHAR(50) NULL,
        [status] VARCHAR(30) NOT NULL CONSTRAINT [DF_nic_app_status] DEFAULT ('SUBMITTED'),
        [is_draft] BIT NOT NULL CONSTRAINT [DF_nic_app_is_draft] DEFAULT (0),
        [payment_status] VARCHAR(20) NOT NULL CONSTRAINT [DF_nic_app_payment_status] DEFAULT ('PENDING'),
        [fee_amount] FLOAT NOT NULL CONSTRAINT [DF_nic_app_fee_amount] DEFAULT (500.0),
        [officer_comment] VARCHAR(500) NULL,
        [created_at] DATETIME2(7) NOT NULL CONSTRAINT [DF_nic_app_created_at] DEFAULT (GETDATE()),
        [updated_at] DATETIME2(7) NOT NULL CONSTRAINT [DF_nic_app_updated_at] DEFAULT (GETDATE()),
        CONSTRAINT [PK_nic_applications] PRIMARY KEY CLUSTERED ([id] ASC),
        CONSTRAINT [FK_nic_applications_users] FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([id])
    );
    PRINT 'Created table: nic_applications';
END
GO

-- 5. Driving License Applications Table
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = N'license_applications')
BEGIN
    CREATE TABLE [dbo].[license_applications] (
        [id] BIGINT IDENTITY(1,1) NOT NULL,
        [user_id] BIGINT NOT NULL,
        [category] VARCHAR(20) NOT NULL,
        [reference_number] VARCHAR(30) NOT NULL UNIQUE,
        [existing_license_number] VARCHAR(30) NULL,
        [issued_license_number] VARCHAR(30) NULL,
        [full_name] VARCHAR(100) NOT NULL,
        [date_of_birth] DATE NOT NULL,
        [address] VARCHAR(255) NOT NULL,
        [phone] VARCHAR(20) NOT NULL,
        [vehicle_class] VARCHAR(50) NOT NULL,
        [medical_cert_required] BIT NOT NULL CONSTRAINT [DF_license_app_med_cert] DEFAULT (0),
        [police_report_ref] VARCHAR(50) NULL,
        [status] VARCHAR(30) NOT NULL CONSTRAINT [DF_license_app_status] DEFAULT ('SUBMITTED'),
        [is_draft] BIT NOT NULL CONSTRAINT [DF_license_app_is_draft] DEFAULT (0),
        [officer_comment] VARCHAR(500) NULL,
        [created_at] DATETIME2(7) NOT NULL CONSTRAINT [DF_license_app_created_at] DEFAULT (GETDATE()),
        [updated_at] DATETIME2(7) NOT NULL CONSTRAINT [DF_license_app_updated_at] DEFAULT (GETDATE()),
        CONSTRAINT [PK_license_applications] PRIMARY KEY CLUSTERED ([id] ASC),
        CONSTRAINT [FK_license_applications_users] FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([id])
    );
    PRINT 'Created table: license_applications';
END
GO

-- 6. Passport Applications Table
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = N'passport_applications')
BEGIN
    CREATE TABLE [dbo].[passport_applications] (
        [id] BIGINT IDENTITY(1,1) NOT NULL,
        [user_id] BIGINT NOT NULL,
        [category] VARCHAR(20) NOT NULL,
        [reference_number] VARCHAR(30) NOT NULL UNIQUE,
        [existing_passport_number] VARCHAR(20) NULL,
        [issued_passport_number] VARCHAR(20) NULL,
        [full_name] VARCHAR(100) NOT NULL,
        [date_of_birth] DATE NOT NULL,
        [place_of_birth] VARCHAR(100) NOT NULL,
        [nationality] VARCHAR(50) NOT NULL CONSTRAINT [DF_passport_app_nationality] DEFAULT ('Sri Lankan'),
        [gender] VARCHAR(20) NOT NULL,
        [address] VARCHAR(255) NOT NULL,
        [phone] VARCHAR(20) NOT NULL,
        [profession] VARCHAR(100) NULL,
        [dual_citizenship] BIT NOT NULL CONSTRAINT [DF_passport_app_dual_cit] DEFAULT (0),
        [police_report_ref] VARCHAR(50) NULL,
        [status] VARCHAR(30) NOT NULL CONSTRAINT [DF_passport_app_status] DEFAULT ('SUBMITTED'),
        [is_draft] BIT NOT NULL CONSTRAINT [DF_passport_app_is_draft] DEFAULT (0),
        [officer_comment] VARCHAR(500) NULL,
        [created_at] DATETIME2(7) NOT NULL CONSTRAINT [DF_passport_app_created_at] DEFAULT (GETDATE()),
        [updated_at] DATETIME2(7) NOT NULL CONSTRAINT [DF_passport_app_updated_at] DEFAULT (GETDATE()),
        CONSTRAINT [PK_passport_applications] PRIMARY KEY CLUSTERED ([id] ASC),
        CONSTRAINT [FK_passport_applications_users] FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([id])
    );
    PRINT 'Created table: passport_applications';
END
GO

-- 7. Payment Transactions Table
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = N'payment_transactions')
BEGIN
    CREATE TABLE [dbo].[payment_transactions] (
        [id] BIGINT IDENTITY(1,1) NOT NULL,
        [transaction_id] VARCHAR(50) NOT NULL UNIQUE,
        [user_id] BIGINT NOT NULL,
        [application_type] VARCHAR(20) NOT NULL,
        [application_id] BIGINT NOT NULL,
        [application_reference] VARCHAR(50) NOT NULL,
        [amount] FLOAT NOT NULL,
        [payment_method] VARCHAR(50) NOT NULL,
        [card_last_four] VARCHAR(10) NOT NULL,
        [payment_status] VARCHAR(20) NOT NULL,
        [failure_reason] VARCHAR(500) NULL,
        [paid_at] DATETIME2(7) NULL,
        [created_at] DATETIME2(7) NOT NULL CONSTRAINT [DF_payment_created_at] DEFAULT (GETDATE()),
        CONSTRAINT [PK_payment_transactions] PRIMARY KEY CLUSTERED ([id] ASC),
        CONSTRAINT [FK_payment_transactions_users] FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([id])
    );
    PRINT 'Created table: payment_transactions';
END
GO

-- 8. Supporting Documents Table (Binary MS SQL Storage)
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = N'application_documents')
BEGIN
    CREATE TABLE [dbo].[application_documents] (
        [id] BIGINT IDENTITY(1,1) NOT NULL,
        [application_id] BIGINT NOT NULL,
        [application_type] VARCHAR(20) NOT NULL,
        [document_type] VARCHAR(50) NOT NULL,
        [document_name] VARCHAR(255) NOT NULL,
        [content_type] VARCHAR(100) NOT NULL,
        [file_data] VARBINARY(MAX) NOT NULL,
        [created_at] DATETIME2(7) NOT NULL CONSTRAINT [DF_app_docs_created_at] DEFAULT (GETDATE()),
        CONSTRAINT [PK_application_documents] PRIMARY KEY CLUSTERED ([id] ASC)
    );
    PRINT 'Created table: application_documents';
END
GO

-- 9. In-App Notifications Table
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = N'notifications')
BEGIN
    CREATE TABLE [dbo].[notifications] (
        [id] BIGINT IDENTITY(1,1) NOT NULL,
        [user_id] BIGINT NOT NULL,
        [subject] VARCHAR(255) NOT NULL,
        [message] VARCHAR(MAX) NOT NULL,
        [type] VARCHAR(50) NOT NULL CONSTRAINT [DF_notif_type] DEFAULT ('EMAIL'),
        [is_read] BIT NOT NULL CONSTRAINT [DF_notif_is_read] DEFAULT (0),
        [sent_at] DATETIME2(7) NOT NULL CONSTRAINT [DF_notif_sent_at] DEFAULT (GETDATE()),
        CONSTRAINT [PK_notifications] PRIMARY KEY CLUSTERED ([id] ASC),
        CONSTRAINT [FK_notifications_users] FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([id])
    );
    PRINT 'Created table: notifications';
END
GO

-- 10. Audit Logs Table
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = N'audit_logs')
BEGIN
    CREATE TABLE [dbo].[audit_logs] (
        [id] BIGINT IDENTITY(1,1) NOT NULL,
        [actor_email] VARCHAR(150) NOT NULL,
        [action] VARCHAR(100) NOT NULL,
        [entity_name] VARCHAR(100) NOT NULL,
        [entity_id] VARCHAR(100) NOT NULL,
        [details] VARCHAR(MAX) NULL,
        [ip_address] VARCHAR(50) NULL,
        [timestamp] DATETIME2(7) NOT NULL CONSTRAINT [DF_audit_timestamp] DEFAULT (GETDATE()),
        CONSTRAINT [PK_audit_logs] PRIMARY KEY CLUSTERED ([id] ASC)
    );
    PRINT 'Created table: audit_logs';
END
GO

-- 11. OTP Tokens Table
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = N'otp_tokens')
BEGIN
    CREATE TABLE [dbo].[otp_tokens] (
        [id] BIGINT IDENTITY(1,1) NOT NULL,
        [user_id] BIGINT NOT NULL,
        [otp_code] VARCHAR(10) NOT NULL,
        [otp_type] VARCHAR(50) NOT NULL,
        [expires_at] DATETIME2(7) NOT NULL,
        [is_used] BIT NOT NULL CONSTRAINT [DF_otp_is_used] DEFAULT (0),
        [created_at] DATETIME2(7) NOT NULL CONSTRAINT [DF_otp_created_at] DEFAULT (GETDATE()),
        CONSTRAINT [PK_otp_tokens] PRIMARY KEY CLUSTERED ([id] ASC),
        CONSTRAINT [FK_otp_tokens_users] FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([id])
    );
    PRINT 'Created table: otp_tokens';
END
GO

-- 12. Spring Session JDBC Tables
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = N'SPRING_SESSION')
BEGIN
    CREATE TABLE [dbo].[SPRING_SESSION] (
        [PRIMARY_ID] CHAR(36) NOT NULL,
        [SESSION_ID] CHAR(36) NOT NULL,
        [CREATION_TIME] BIGINT NOT NULL,
        [LAST_ACCESS_TIME] BIGINT NOT NULL,
        [MAX_INACTIVE_INTERVAL] INT NOT NULL,
        [EXPIRY_TIME] BIGINT NOT NULL,
        [PRINCIPAL_NAME] VARCHAR(100) NULL,
        CONSTRAINT [SPRING_SESSION_PK] PRIMARY KEY CLUSTERED ([PRIMARY_ID] ASC)
    );
    CREATE UNIQUE NONCLUSTERED INDEX [SPRING_SESSION_IX1] ON [dbo].[SPRING_SESSION] ([SESSION_ID] ASC);
    CREATE NONCLUSTERED INDEX [SPRING_SESSION_IX2] ON [dbo].[SPRING_SESSION] ([EXPIRY_TIME] ASC);
    CREATE NONCLUSTERED INDEX [SPRING_SESSION_IX3] ON [dbo].[SPRING_SESSION] ([PRINCIPAL_NAME] ASC);
    PRINT 'Created table: SPRING_SESSION';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = N'SPRING_SESSION_ATTRIBUTES')
BEGIN
    CREATE TABLE [dbo].[SPRING_SESSION_ATTRIBUTES] (
        [SESSION_PRIMARY_ID] CHAR(36) NOT NULL,
        [ATTRIBUTE_NAME] VARCHAR(200) NOT NULL,
        [ATTRIBUTE_BYTES] VARBINARY(MAX) NOT NULL,
        CONSTRAINT [SPRING_SESSION_ATTRIBUTES_PK] PRIMARY KEY CLUSTERED ([SESSION_PRIMARY_ID] ASC, [ATTRIBUTE_NAME] ASC),
        CONSTRAINT [SPRING_SESSION_ATTRIBUTES_FK] FOREIGN KEY ([SESSION_PRIMARY_ID]) REFERENCES [dbo].[SPRING_SESSION] ([PRIMARY_ID]) ON DELETE CASCADE
    );
    PRINT 'Created table: SPRING_SESSION_ATTRIBUTES';
END
GO

-- ============================================================================
-- STEP 4: SEED ROLES & USERS (IF NOT EXISTS)
-- All accounts have default password: Password123
-- BCrypt hash: $2a$10$qWNTmABu3DexmEsZ6C1eOOFreHvF9Kqp5hyvIOP9WA/f2CUT3HckW
-- ============================================================================

-- Seed Roles
IF NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'APPLICANT')
    INSERT INTO roles (role_name) VALUES ('APPLICANT');
GO
IF NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'VERIFICATION_OFFICER')
    INSERT INTO roles (role_name) VALUES ('VERIFICATION_OFFICER');
GO
IF NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'SUPER_ADMIN')
    INSERT INTO roles (role_name) VALUES ('SUPER_ADMIN');
GO

-- Seed 1: Super Administrator (admin@nidis.gov.lk)
IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@nidis.gov.lk')
    INSERT INTO users (full_name, email, nic_number, password_hash, phone, is_active, is_email_verified, created_at, updated_at)
    VALUES ('System Administrator', 'admin@nidis.gov.lk', '198000000001',
            '$2a$10$qWNTmABu3DexmEsZ6C1eOOFreHvF9Kqp5hyvIOP9WA/f2CUT3HckW',
            '0710000001', 1, 1, GETDATE(), GETDATE());
GO

IF NOT EXISTS (
    SELECT 1 FROM user_roles ur
    JOIN users u ON ur.user_id = u.id
    JOIN roles r ON ur.role_id = r.id
    WHERE u.email = 'admin@nidis.gov.lk' AND r.role_name = 'SUPER_ADMIN'
)
    INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u, roles r
    WHERE u.email = 'admin@nidis.gov.lk' AND r.role_name = 'SUPER_ADMIN';
GO

-- Seed 2: Verification Officer (officer@nidis.gov.lk)
IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'officer@nidis.gov.lk')
    INSERT INTO users (full_name, email, nic_number, password_hash, phone, is_active, is_email_verified, created_at, updated_at)
    VALUES ('Officer Kamal Perera', 'officer@nidis.gov.lk', '198500000002',
            '$2a$10$qWNTmABu3DexmEsZ6C1eOOFreHvF9Kqp5hyvIOP9WA/f2CUT3HckW',
            '0770000002', 1, 1, GETDATE(), GETDATE());
GO

IF NOT EXISTS (
    SELECT 1 FROM user_roles ur
    JOIN users u ON ur.user_id = u.id
    JOIN roles r ON ur.role_id = r.id
    WHERE u.email = 'officer@nidis.gov.lk' AND r.role_name = 'VERIFICATION_OFFICER'
)
    INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u, roles r
    WHERE u.email = 'officer@nidis.gov.lk' AND r.role_name = 'VERIFICATION_OFFICER';
GO

-- Seed 3: Citizen Test User (citizen@test.lk)
IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'citizen@test.lk')
    INSERT INTO users (full_name, email, nic_number, password_hash, phone, is_active, is_email_verified, created_at, updated_at)
    VALUES ('Citizen Test User', 'citizen@test.lk', '200012345678',
            '$2a$10$qWNTmABu3DexmEsZ6C1eOOFreHvF9Kqp5hyvIOP9WA/f2CUT3HckW',
            '0771234567', 1, 1, GETDATE(), GETDATE());
GO

IF NOT EXISTS (
    SELECT 1 FROM user_roles ur
    JOIN users u ON ur.user_id = u.id
    JOIN roles r ON ur.role_id = r.id
    WHERE u.email = 'citizen@test.lk' AND r.role_name = 'APPLICANT'
)
    INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u, roles r
    WHERE u.email = 'citizen@test.lk' AND r.role_name = 'APPLICANT';
GO

-- Seed 4: Citizen 2 (kamal@test.lk)
IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'kamal@test.lk')
    INSERT INTO users (full_name, email, nic_number, password_hash, phone, is_active, is_email_verified, created_at, updated_at)
    VALUES ('Kamal Silva', 'kamal@test.lk', '199515004128',
            '$2a$10$qWNTmABu3DexmEsZ6C1eOOFreHvF9Kqp5hyvIOP9WA/f2CUT3HckW',
            '0719876543', 1, 1, GETDATE(), GETDATE());
GO

IF NOT EXISTS (
    SELECT 1 FROM user_roles ur
    JOIN users u ON ur.user_id = u.id
    JOIN roles r ON ur.role_id = r.id
    WHERE u.email = 'kamal@test.lk' AND r.role_name = 'APPLICANT'
)
    INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u, roles r
    WHERE u.email = 'kamal@test.lk' AND r.role_name = 'APPLICANT';
GO

-- ============================================================================
-- STEP 5: SEED SAMPLE APPLICATIONS WITH OFFICIAL SRI LANKAN NUMBERS
-- ============================================================================

-- 1. Sample Shipped Smart NIC with issued 12-digit number (200012345678)
IF NOT EXISTS (SELECT 1 FROM nic_applications WHERE reference_number = 'NIC-2026-DEMO01')
BEGIN
    DECLARE @CitizenId BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'citizen@test.lk');
    INSERT INTO nic_applications (
        user_id, category, reference_number, existing_nic_number, issued_nic_number,
        full_name, date_of_birth, gender, civil_status, occupation, permanent_address,
        phone, grama_niladhari_division, divisional_secretariat, status, is_draft,
        payment_status, fee_amount, officer_comment, created_at, updated_at
    ) VALUES (
        @CitizenId, 'NEW', 'NIC-2026-DEMO01', NULL, '200012345678',
        'Citizen Test User', '2000-05-02', 'Male', 'Single', 'Software Engineer',
        'No. 45/2, Galle Road, Colombo 03', '0771234567', 'Kollupitiya North 514A',
        'Colombo', 'SHIPPED', 0, 'PAID', 500.0,
        'Biometrics and birth certificate verified. Smart NIC issued and dispatched.',
        DATEADD(DAY, -5, GETDATE()), GETDATE()
    );
END
GO

-- 2. Sample NIC under review (officer review queue item)
IF NOT EXISTS (SELECT 1 FROM nic_applications WHERE reference_number = 'NIC-2026-DEMO02')
BEGIN
    DECLARE @KamalId BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'kamal@test.lk');
    INSERT INTO nic_applications (
        user_id, category, reference_number, existing_nic_number, issued_nic_number,
        full_name, date_of_birth, gender, civil_status, occupation, permanent_address,
        phone, grama_niladhari_division, divisional_secretariat, status, is_draft,
        payment_status, fee_amount, officer_comment, created_at, updated_at
    ) VALUES (
        @KamalId, 'RENEWAL', 'NIC-2026-DEMO02', '199515004128', NULL,
        'Kamal Silva', '1995-05-30', 'Male', 'Married', 'Accountant',
        'No. 12, Kandy Road, Kiribathgoda', '0719876543', 'Kiribathgoda West',
        'Kelaniya', 'UNDER_REVIEW', 0, 'PAID', 500.0,
        'Pending officer document review.',
        DATEADD(DAY, -1, GETDATE()), GETDATE()
    );
END
GO

-- 3. Sample Shipped Driving License with issued DMT number (B8294105)
IF NOT EXISTS (SELECT 1 FROM license_applications WHERE reference_number = 'LIC-2026-DEMO01')
BEGIN
    DECLARE @CitizenId2 BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'citizen@test.lk');
    INSERT INTO license_applications (
        user_id, category, reference_number, existing_license_number, issued_license_number,
        full_name, date_of_birth, address, phone, vehicle_class, medical_cert_required,
        status, is_draft, officer_comment, created_at, updated_at
    ) VALUES (
        @CitizenId2, 'NEW', 'LIC-2026-DEMO01', NULL, 'B8294105',
        'Citizen Test User', '2000-05-02', 'No. 45/2, Galle Road, Colombo 03', '0771234567',
        'B, B1 (Light Vehicles & Auto)', 1, 'SHIPPED', 0,
        'Practical and medical certified. License card dispatched.',
        DATEADD(DAY, -4, GETDATE()), GETDATE()
    );
END
GO

-- 4. Sample Approved & Paid Driving License ready for officer Verify Payment & Dispatch testing
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
        'Officer verified bio-data and NTMI medical certificate. Approved.',
        DATEADD(DAY, -2, GETDATE()), GETDATE()
    );
END
GO

-- 5. Sample Shipped Passport with issued DIE number (N7482910)
IF NOT EXISTS (SELECT 1 FROM passport_applications WHERE reference_number = 'PASS-2026-DEMO01')
BEGIN
    DECLARE @CitizenId3 BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'citizen@test.lk');
    INSERT INTO passport_applications (
        user_id, category, reference_number, existing_passport_number, issued_passport_number,
        full_name, date_of_birth, place_of_birth, nationality, gender, address, phone,
        profession, dual_citizenship, status, is_draft, officer_comment, created_at, updated_at
    ) VALUES (
        @CitizenId3, 'NEW', 'PASS-2026-DEMO01', NULL, 'N7482910',
        'Citizen Test User', '2000-05-02', 'Colombo', 'Sri Lankan', 'Male',
        'No. 45/2, Galle Road, Colombo 03', '0771234567', 'Software Engineer', 0,
        'SHIPPED', 0, 'ICAO biometric passport approved, printed and shipped via postal courier.',
        DATEADD(DAY, -3, GETDATE()), GETDATE()
    );
END
GO

-- Seed Payment Transactions for the applications
IF NOT EXISTS (SELECT 1 FROM payment_transactions WHERE transaction_id = 'TXN-DEMO-NIC01')
BEGIN
    DECLARE @CitizenId4 BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'citizen@test.lk');
    DECLARE @NicAppId BIGINT = (SELECT TOP 1 id FROM nic_applications WHERE reference_number = 'NIC-2026-DEMO01');
    INSERT INTO payment_transactions (
        transaction_id, user_id, application_type, application_id, application_reference,
        amount, payment_method, card_last_four, payment_status, paid_at, created_at
    ) VALUES (
        'TXN-DEMO-NIC01', @CitizenId4, 'NIC', @NicAppId, 'NIC-2026-DEMO01',
        500.0, 'VISA/MASTERCARD', '4242', 'PAID', DATEADD(DAY, -5, GETDATE()), DATEADD(DAY, -5, GETDATE())
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM payment_transactions WHERE transaction_id = 'TXN-DEMO-LIC01')
BEGIN
    DECLARE @CitizenId5 BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'citizen@test.lk');
    DECLARE @LicAppId BIGINT = (SELECT TOP 1 id FROM license_applications WHERE reference_number = 'LIC-2026-DEMO01');
    INSERT INTO payment_transactions (
        transaction_id, user_id, application_type, application_id, application_reference,
        amount, payment_method, card_last_four, payment_status, paid_at, created_at
    ) VALUES (
        'TXN-DEMO-LIC01', @CitizenId5, 'LICENSE', @LicAppId, 'LIC-2026-DEMO01',
        2500.0, 'VISA/MASTERCARD', '4242', 'PAID', DATEADD(DAY, -4, GETDATE()), DATEADD(DAY, -4, GETDATE())
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM payment_transactions WHERE transaction_id = 'TXN-DEMO-LIC02')
BEGIN
    DECLARE @KamalId3 BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'kamal@test.lk');
    DECLARE @LicAppId2 BIGINT = (SELECT TOP 1 id FROM license_applications WHERE reference_number = 'LIC-2026-DEMO02');
    INSERT INTO payment_transactions (
        transaction_id, user_id, application_type, application_id, application_reference,
        amount, payment_method, card_last_four, payment_status, paid_at, created_at
    ) VALUES (
        'TXN-DEMO-LIC02', @KamalId3, 'LICENSE', @LicAppId2, 'LIC-2026-DEMO02',
        2500.0, 'VISA/MASTERCARD', '1111', 'PAID', DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -1, GETDATE())
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM payment_transactions WHERE transaction_id = 'TXN-DEMO-PASS01')
BEGIN
    DECLARE @CitizenId6 BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'citizen@test.lk');
    DECLARE @PassAppId BIGINT = (SELECT TOP 1 id FROM passport_applications WHERE reference_number = 'PASS-2026-DEMO01');
    INSERT INTO payment_transactions (
        transaction_id, user_id, application_type, application_id, application_reference,
        amount, payment_method, card_last_four, payment_status, paid_at, created_at
    ) VALUES (
        'TXN-DEMO-PASS01', @CitizenId6, 'PASSPORT', @PassAppId, 'PASS-2026-DEMO01',
        5000.0, 'VISA/MASTERCARD', '4242', 'PAID', DATEADD(DAY, -3, GETDATE()), DATEADD(DAY, -3, GETDATE())
    );
END
GO

-- Seed Notifications
IF NOT EXISTS (SELECT 1 FROM notifications WHERE subject LIKE '%NIC-2026-DEMO01%')
BEGIN
    DECLARE @CitizenId7 BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'citizen@test.lk');
    INSERT INTO notifications (user_id, subject, message, type, is_read, sent_at)
    VALUES (
        @CitizenId7,
        'Payment Verified & National Identity Card (NIC) Shipped (200012345678)',
        'Your payment for NIC-2026-DEMO01 has been verified by the officer. Your official National Identity Card (Number: 200012345678) has been issued and dispatched. Please securely record this number for any future renewals or lost replacement applications.',
        'EMAIL', 0, DATEADD(DAY, -5, GETDATE())
    );
END
GO

-- Seed Audit Logs
IF NOT EXISTS (SELECT 1 FROM audit_logs WHERE entity_id = 'NIC-2026-DEMO01')
BEGIN
    INSERT INTO audit_logs (actor_email, action, entity_name, entity_id, details, ip_address, timestamp)
    VALUES (
        'officer@nidis.gov.lk', 'APPLICATION_SHIPPED', 'NIC', 'NIC-2026-DEMO01',
        'Officer Officer Kamal Perera set status to SHIPPED with official number: 200012345678. Remarks: Smart NIC dispatched.',
        '127.0.0.1', DATEADD(DAY, -5, GETDATE())
    );
END
GO

PRINT '========================================================================';
PRINT ' NIDIS Database setup and seed data execution completed successfully! ';
PRINT '========================================================================';
GO

