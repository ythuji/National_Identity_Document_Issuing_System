package org.nidis.national_identity_document_issuing_system.config;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.nidis.national_identity_document_issuing_system.model.ApplicationDocument;
import org.nidis.national_identity_document_issuing_system.model.AuditLog;
import org.nidis.national_identity_document_issuing_system.model.LicenseApplication;
import org.nidis.national_identity_document_issuing_system.model.NicApplication;
import org.nidis.national_identity_document_issuing_system.model.PassportApplication;
import org.nidis.national_identity_document_issuing_system.model.PaymentTransaction;
import org.nidis.national_identity_document_issuing_system.model.Role;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationCategory;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationStatus;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;
import org.nidis.national_identity_document_issuing_system.model.enums.PaymentStatus;
import org.nidis.national_identity_document_issuing_system.model.enums.RoleName;
import org.nidis.national_identity_document_issuing_system.repository.ApplicationDocumentRepository;
import org.nidis.national_identity_document_issuing_system.repository.AuditLogRepository;
import org.nidis.national_identity_document_issuing_system.repository.LicenseApplicationRepository;
import org.nidis.national_identity_document_issuing_system.repository.NicApplicationRepository;
import org.nidis.national_identity_document_issuing_system.repository.PassportApplicationRepository;
import org.nidis.national_identity_document_issuing_system.repository.PaymentTransactionRepository;
import org.nidis.national_identity_document_issuing_system.repository.RoleRepository;
import org.nidis.national_identity_document_issuing_system.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final LicenseApplicationRepository licenseRepository;
    private final PassportApplicationRepository passportRepository;
    private final NicApplicationRepository nicRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final ApplicationDocumentRepository documentRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing system seed data and demo records...");

        // 0. Drop any outdated check constraints on otp_tokens.type in SQL Server
        try {
            jdbcTemplate.execute(
                "DECLARE @sql NVARCHAR(MAX) = N''; " +
                "SELECT @sql += N'ALTER TABLE dbo.otp_tokens DROP CONSTRAINT ' + QUOTENAME(name) + ';' " +
                "FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID('dbo.otp_tokens'); " +
                "IF LEN(@sql) > 0 EXEC sp_executesql @sql;"
            );
        } catch (Exception ex) {
            log.warn("Notice: OTP token check constraint drop attempted ({})", ex.getMessage());
        }

        // 1. Ensure Roles Exist
        Role applicantRole = getOrCreateRole(RoleName.APPLICANT);
        Role officerRole = getOrCreateRole(RoleName.VERIFICATION_OFFICER);
        Role adminRole = getOrCreateRole(RoleName.SUPER_ADMIN);

        // 2. Ensure Users Exist
        User adminUser = getOrCreateUser("admin@nidis.gov.lk", "System Administrator", "198000000001", "0710000001", adminRole);
        User officerUser = getOrCreateUser("officer@nidis.gov.lk", "Officer Kamal Perera", "198500000002", "0770000002", officerRole);
        User citizenUser = getOrCreateUser("citizen@test.lk", "Citizen Test User", "200012345678", "0771234567", applicantRole);
        User devUser = getOrCreateUser("yogathuji24@gmail.com", "Yogathas Thujikoshan", "200113603450", "0771234567", applicantRole);

        // 3. Seed Sample Applications for Citizen if none exist
        if (nicRepository.findByUserIdOrderByCreatedAtDesc(citizenUser.getId()).isEmpty()) {
            seedSampleNicApplications(citizenUser);
        }

        if (licenseRepository.findByUserIdOrderByCreatedAtDesc(citizenUser.getId()).isEmpty()) {
            seedSampleLicenseApplications(citizenUser);
        }

        if (passportRepository.findByUserIdOrderByCreatedAtDesc(citizenUser.getId()).isEmpty()) {
            seedSamplePassportApplications(citizenUser);
        }

        // 4. Seed Sample Audit Logs if none exist
        if (auditLogRepository.count() == 0) {
            seedSampleAuditLogs();
        }

        log.info("System seed data and demo records initialization complete.");
    }

    private Role getOrCreateRole(RoleName name) {
        return roleRepository.findByRoleName(name).orElseGet(() ->
                roleRepository.save(Role.builder().roleName(name).build()));
    }

    private User getOrCreateUser(String email, String fullName, String nic, String phone, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = User.builder()
                    .email(email)
                    .fullName(fullName)
                    .nicNumber(nic)
                    .phone(phone)
                    .passwordHash(passwordEncoder.encode("Password123"))
                    .isActive(true)
                    .isEmailVerified(true)
                    .build();
            user.getRoles().add(role);
            return userRepository.save(user);
        });
    }

    private void seedSampleNicApplications(User user) {
        NicApplication nicApp = NicApplication.builder()
                .user(user)
                .category(ApplicationCategory.NEW)
                .referenceNumber("NIC-20260919-80123")
                .fullName(user.getFullName())
                .dateOfBirth(LocalDate.of(2000, 3, 20))
                .gender("Male")
                .civilStatus("Single")
                .occupation("Software Engineer")
                .permanentAddress("No. 120, Galle Road, Colombo 03")
                .phone(user.getPhone())
                .gramaNiladhariDivision("520B Kollupitiya")
                .divisionalSecretariat("Colombo")
                .status(ApplicationStatus.UNDER_REVIEW)
                .paymentStatus(PaymentStatus.PAID)
                .feeAmount(500.0)
                .officerComment("Documents under cross-verification with Grama Niladhari division.")
                .build();

        NicApplication savedNic = nicRepository.save(nicApp);

        // Seed Sample Binary Document
        documentRepository.save(ApplicationDocument.builder()
                .applicationId(savedNic.getId())
                .applicationType(ApplicationType.NIC)
                .documentName("citizen_photo.jpg")
                .documentType("PHOTO")
                .contentType("image/jpeg")
                .fileData("SAMPLE_MOCK_IMAGE_DATA_BYTES_FOR_NIC".getBytes())
                .fileSize(1024L)
                .build());

        // Seed Payment Transaction
        paymentRepository.save(PaymentTransaction.builder()
                .transactionId("TXN-20260919-10091")
                .user(user)
                .applicationType(ApplicationType.NIC)
                .applicationId(savedNic.getId())
                .applicationReference(savedNic.getReferenceNumber())
                .amount(500.0)
                .paymentMethod("CARD")
                .cardLastFour("4242")
                .paymentStatus(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now().minusHours(2))
                .build());
    }

    private void seedSampleLicenseApplications(User user) {
        LicenseApplication licApp = LicenseApplication.builder()
                .user(user)
                .category(ApplicationCategory.NEW)
                .referenceNumber("LIC-20260919-50231")
                .fullName(user.getFullName())
                .dateOfBirth(LocalDate.of(2000, 3, 20))
                .address("No. 120, Galle Road, Colombo 03")
                .phone(user.getPhone())
                .vehicleClass("B (Car/Dual Purpose), A (Motorcycle)")
                .medicalCertRequired(true)
                .status(ApplicationStatus.APPROVED)
                .officerComment("Medical fitness certificate and practical driving test passed. Approved.")
                .build();

        LicenseApplication savedLic = licenseRepository.save(licApp);

        // Seed Sample Binary Document
        documentRepository.save(ApplicationDocument.builder()
                .applicationId(savedLic.getId())
                .applicationType(ApplicationType.LICENSE)
                .documentName("medical_report.pdf")
                .documentType("MEDICAL_CERT")
                .contentType("application/pdf")
                .fileData("SAMPLE_MOCK_PDF_DATA_BYTES_FOR_MEDICAL".getBytes())
                .fileSize(2048L)
                .build());

        // Seed Payment Transaction
        paymentRepository.save(PaymentTransaction.builder()
                .transactionId("TXN-20260919-20082")
                .user(user)
                .applicationType(ApplicationType.LICENSE)
                .applicationId(savedLic.getId())
                .applicationReference(savedLic.getReferenceNumber())
                .amount(2500.0)
                .paymentMethod("CARD")
                .cardLastFour("4242")
                .paymentStatus(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now().minusDays(1))
                .build());
    }

    private void seedSamplePassportApplications(User user) {
        PassportApplication passApp = PassportApplication.builder()
                .user(user)
                .category(ApplicationCategory.RENEWAL)
                .referenceNumber("PPT-20260919-90412")
                .existingPassportNumber("N8912345")
                .fullName(user.getFullName())
                .dateOfBirth(LocalDate.of(2000, 3, 20))
                .placeOfBirth("Colombo")
                .nationality("Sri Lankan")
                .gender("Male")
                .address("No. 120, Galle Road, Colombo 03")
                .phone(user.getPhone())
                .profession("Software Engineer")
                .dualCitizenship(false)
                .status(ApplicationStatus.SUBMITTED)
                .build();

        PassportApplication savedPass = passportRepository.save(passApp);

        // Seed Sample Binary Document
        documentRepository.save(ApplicationDocument.builder()
                .applicationId(savedPass.getId())
                .applicationType(ApplicationType.PASSPORT)
                .documentName("passport_studio_photo.jpg")
                .documentType("PHOTO")
                .contentType("image/jpeg")
                .fileData("SAMPLE_MOCK_IMAGE_DATA_BYTES_FOR_PASSPORT".getBytes())
                .fileSize(1024L)
                .build());
    }

    private void seedSampleAuditLogs() {
        auditLogRepository.save(AuditLog.builder()
                .actorEmail("system@nidis.gov.lk")
                .action("SYSTEM_INITIALIZED")
                .entityName("SYSTEM")
                .entityId("CORE")
                .details("NIDIS Database Schema and Security Context initialized.")
                .ipAddress("127.0.0.1")
                .timestamp(LocalDateTime.now().minusDays(2))
                .build());

        auditLogRepository.save(AuditLog.builder()
                .actorEmail("admin@nidis.gov.lk")
                .action("ROLES_CONFIGURED")
                .entityName("USER")
                .entityId("officer@nidis.gov.lk")
                .details("Assigned role VERIFICATION_OFFICER to staff member.")
                .ipAddress("127.0.0.1")
                .timestamp(LocalDateTime.now().minusDays(1))
                .build());

        auditLogRepository.save(AuditLog.builder()
                .actorEmail("officer@nidis.gov.lk")
                .action("APPLICATION_APPROVED")
                .entityName("LICENSE")
                .entityId("LIC-20260919-50231")
                .details("Approved Driving License application after verifying medical examination reports.")
                .ipAddress("127.0.0.1")
                .timestamp(LocalDateTime.now().minusHours(4))
                .build());
    }
}

