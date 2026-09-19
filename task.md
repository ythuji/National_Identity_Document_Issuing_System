# NIDIS — Project Implementation Progress

## Step 0 — Project Foundation & Shared Infrastructure [COMPLETED]
- [x] 0.1 — Update `pom.xml` (add security, validation, mail, thymeleaf-extras-springsecurity6)
- [x] 0.2 — Create 6 enum classes (`RoleName`, `ApplicationCategory`, `ApplicationStatus`, `ApplicationType`, `PaymentStatus`, `OtpType`)
- [x] 0.3 — Create `Role.java` entity
- [x] 0.4 — Create `User.java` entity
- [x] 0.5 — Create `Notification.java` entity
- [x] 0.6 — Create `RoleRepository.java`
- [x] 0.7 — Create `UserRepository.java`
- [x] 0.8 — Create `NotificationRepository.java`
- [x] 0.9 — Create `SecurityConfig.java`
- [x] 0.10 — Create `NotificationService.java`
- [x] 0.11 — Create `data.sql` (Seed roles and default Super Admin)
- [x] 0.12 — Update `application.properties` for database, security, and email
- [x] 0.13 — Create `header.html` fragment
- [x] 0.14 — Create `footer.html` fragment
- [x] 0.15 — Create `sidebar.html` fragment
- [x] 0.16 — Create `style.css`
- [x] 0.17 — Verified build & database startup with MS SQL Server

---

## Step 1 — Register Account & Login (UC-01) — Member 1: Hettiarachchi [COMPLETED]
- [x] 1.1 — Create `OtpToken.java` entity (package `model`)
- [x] 1.2 — Create `OtpTokenRepository.java` (package `repository`)
- [x] 1.3 — Create DTOs: `RegistrationDto.java`, `LoginDto.java`, `ResetPasswordDto.java` (package `dto`)
- [x] 1.4 — Create `EmailService.java` (package `service`)
- [x] 1.5 — Create `OtpService.java` (package `service`)
- [x] 1.6 — Create `UserService.java` (package `service`)
- [x] 1.7 — Create `AuthService.java` implementing `UserDetailsService` (package `service`)
- [x] 1.8 — Update `SecurityConfig.java` to support `.usernameParameter("email")`
- [x] 1.9 — Create `AuthController.java` (package `controller`)
- [x] 1.10 — Create `DashboardController.java` (package `controller`)
- [x] 1.11 — Create Thymeleaf templates (`register.html`, `verify-otp.html`, `login.html`, `forgot-password.html`, `reset-password.html`, `dashboard/index.html`)
- [x] 1.12 — End-to-end authentication verified

---

## Step 2 — Driver's License Applications (UC-02) — Member 2: Thujikoshan [COMPLETED]
- [x] 2.1 — Create `LicenseApplication.java` entity and `ApplicationDocument.java` (`VARBINARY(MAX)` in MS SQL Server)
- [x] 2.2 — Create `LicenseApplicationRepository.java` and `ApplicationDocumentRepository.java`
- [x] 2.3 — Create `LicenseApplicationDto.java`
- [x] 2.4 — Create `LicenseService.java` and `FileStorageService.java`
- [x] 2.5 — Create `LicenseController.java` with document streaming and prefill endpoints
- [x] 2.6 — Create Thymeleaf templates (`select-category.html`, `apply-new.html`, `apply-renewal.html`, `apply-lost.html`, `application-detail.html`, `my-applications.html`)

---

## Step 3 — Passport Applications (UC-03) — Member 3: Chandrasekara [COMPLETED]
- [x] 3.1 — Create `PassportApplication.java` entity
- [x] 3.2 — Create `PassportApplicationRepository.java`
- [x] 3.3 — Create `PassportApplicationDto.java`
- [x] 3.4 — Create `PassportService.java`
- [x] 3.5 — Create `PassportController.java` with document streaming and bio-data prefill endpoints
- [x] 3.6 — Create Thymeleaf templates (`select-category.html`, `apply-new.html`, `apply-renewal.html`, `apply-lost.html`, `application-detail.html`, `my-applications.html`)

---

## Step 4 — National Identity Card (NIC) (UC-04) — Member 4: Rajapaksha P.A.D.S [COMPLETED]
- [x] 4.1 — Create `NicApplication.java` entity (with `PaymentStatus` and `feeAmount`)
- [x] 4.2 — Create `NicApplicationRepository.java`
- [x] 4.3 — Create `NicApplicationDto.java`
- [x] 4.4 — Create `NicService.java` (reference generation `NIC-YYYYMMDD-XXXXX`, drafts, renewals, lost replacements, file uploads)
- [x] 4.5 — Create `NicController.java` (`/nic`, `/nic/apply/{category}`, `/nic/draft`, `/nic/view/{id}`, `/nic/my-applications`, `/nic/document/{docId}`, `/nic/prefill`)
- [x] 4.6 — Create Thymeleaf templates (`select-category.html`, `apply-new.html`, `apply-renewal.html`, `apply-lost.html`, `application-detail.html`, `my-applications.html`)
- [x] 4.7 — Integrate NIC statistics & items into `DashboardController.java`

---

## Step 5 — Generic Unified Payment Gateway — Member 4: Rajapaksha P.A.D.S [COMPLETED]
- [x] 5.1 — Create `PaymentTransaction.java` entity (`transactionId`, `applicationType`, `amount`, `paymentStatus`, `paidAt`)
- [x] 5.2 — Create `PaymentTransactionRepository.java`
- [x] 5.3 — Create `PaymentRequestDto.java`
- [x] 5.4 — Create `PaymentService.java` (fee calculation, card processing, status updating to `UNDER_REVIEW`, notification dispatch)
- [x] 5.5 — Create `PaymentController.java` (`/payment/checkout/{type}/{id}`, `/payment/process`, `/payment/receipt/{txnId}`, `/payment/my-payments`)
- [x] 5.6 — Create Thymeleaf templates (`checkout.html`, `receipt.html`, `my-payments.html`)

---

## Step 6 — Verification Queue & Review Engine (UC-05) — Member 5: Unawatuna H.M.I.D [COMPLETED]
- [x] 6.1 — Create `VerificationItemDto.java` & `VerificationDecisionDto.java`
- [x] 6.2 — Create `VerificationService.java` (aggregated queue for License, Passport, NIC; Approve, Reject, Correction actions; audit event logging)
- [x] 6.3 — Create `VerificationController.java` (`/verification/queue`, `/verification/review/{type}/{id}`, `/verification/action`)
- [x] 6.4 — Create Thymeleaf templates (`queue.html`, `review.html` with side-by-side inspection and modal decision workflows)

---

## Step 7 — Super Admin Access Control & Audit Log (UC-06) — Member 6: Karunarathne H.L.N.A [COMPLETED]
- [x] 7.1 — Create `AuditLog.java` entity
- [x] 7.2 — Create `AuditLogRepository.java`
- [x] 7.3 — Create `AuditLogService.java` (recording actions, retrieving security trail)
- [x] 7.4 — Create `AdminUserService.java` (manage accounts, toggle active/suspended status, assign/revoke roles)
- [x] 7.5 — Create `AdminController.java` (`/admin/dashboard`, `/admin/users`, `/admin/users/{id}/toggle-status`, `/admin/users/{id}/roles`, `/admin/audit-log`)
- [x] 7.6 — Create Thymeleaf templates (`dashboard.html`, `users.html`, `audit-log.html`, updated `sidebar.html`)

---

## Step 7.5 — Role-Wise Workflow & Dedicated Dashboard Architecture [COMPLETED]
- [x] 7.5.1 — Create `CustomAuthenticationSuccessHandler.java` for role-based login routing
- [x] 7.5.2 — Update `SecurityConfig.java` with successHandler and route privileges
- [x] 7.5.3 — Update `DashboardController.java` with role-aware dispatching
- [x] 7.5.4 — Create dedicated `AdminDashboard` with KPIs and revenue metrics
- [x] 7.5.5 — Separate navigation bars and headers in `header.html` & `sidebar.html` per role (`APPLICANT`, `VERIFICATION_OFFICER`, `SUPER_ADMIN`)
- [x] 7.5.6 — Ensure consistent Super Admin layout (with Admin sidebar and uniform branding) across Verification Queue and Review pages while preserving full-width dedicated workspace for Verification Officers
- [x] 7.5.6 — Verified with automated tests (`BUILD SUCCESS`) and live HTTP redirect testing

---

## Step 8 — Comprehensive Testing, Polish & Final Documentation [COMPLETED]
- [x] 8.1 — Comprehensive Admin UI, layout, sizing, and readability overhaul:
  - Added `.admin-layout`, `.admin-sidebar` (fixed 250px), and `.admin-content` (`flex: 1; min-width: 0;`).
  - Switched navbar to `.container-fluid px-3 px-md-4` for edge-to-edge alignment with the sidebar and tables.
  - Sized KPI summary cards and revenue display cleanly (`.kpi-card`).
  - Removed hover card jitter (`translateY(-3px)`) on table cards.
  - Replaced monospace font on audit log with clean typography and budgeted column widths.
  - Integrated search, avatar initials, and human-friendly role badges on the user management table.
  - Budgeted 9 columns in the verification queue table with subtle badges.
  - Tested all 5 admin endpoints and officer views with automated HTTP verification script.

