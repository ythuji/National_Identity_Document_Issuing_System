package org.nidis.national_identity_document_issuing_system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.nidis.national_identity_document_issuing_system.dto.NicApplicationDto;
import org.nidis.national_identity_document_issuing_system.dto.PaymentRequestDto;
import org.nidis.national_identity_document_issuing_system.dto.VerificationDecisionDto;
import org.nidis.national_identity_document_issuing_system.dto.VerificationItemDto;
import org.nidis.national_identity_document_issuing_system.model.ApplicationDocument;
import org.nidis.national_identity_document_issuing_system.model.AuditLog;
import org.nidis.national_identity_document_issuing_system.model.NicApplication;
import org.nidis.national_identity_document_issuing_system.model.PaymentTransaction;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationCategory;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationStatus;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;
import org.nidis.national_identity_document_issuing_system.model.enums.PaymentStatus;
import org.nidis.national_identity_document_issuing_system.repository.UserRepository;
import org.nidis.national_identity_document_issuing_system.service.AdminUserService;
import org.nidis.national_identity_document_issuing_system.service.AuditLogService;
import org.nidis.national_identity_document_issuing_system.service.FileStorageService;
import org.nidis.national_identity_document_issuing_system.service.NicService;
import org.nidis.national_identity_document_issuing_system.service.PaymentService;
import org.nidis.national_identity_document_issuing_system.service.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class NationalIdentityDocumentIssuingSystemApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NicService nicService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private FileStorageService fileStorageService;

    @Test
    void contextLoads() {
        assertNotNull(userRepository);
        assertNotNull(nicService);
        assertNotNull(paymentService);
        assertNotNull(verificationService);
        assertNotNull(adminUserService);
        assertNotNull(auditLogService);
    }

    @Test
    @Transactional
    void testEndToEndNicPaymentVerificationFlow() throws Exception {
        // 1. Fetch Citizen user
        User citizen = userRepository.findByEmail("citizen@test.lk")
                .orElseThrow(() -> new IllegalStateException("Test user citizen@test.lk not found"));

        // 2. Submit First-Time NIC Application with mock documents
        MockMultipartFile photo = new MockMultipartFile(
                "photoFile", "passport_photo.jpg", "image/jpeg", "fake_image_bytes_ICAO".getBytes());
        MockMultipartFile birthCert = new MockMultipartFile(
                "birthCertFile", "birth_certificate.pdf", "application/pdf", "fake_pdf_birth_cert_bytes".getBytes());

        NicApplicationDto dto = NicApplicationDto.builder()
                .category(ApplicationCategory.NEW)
                .fullName("YOGATHAS THUJIKOSHAN")
                .dateOfBirth(LocalDate.of(2001, 5, 15))
                .gender("Male")
                .civilStatus("Single")
                .occupation("Software Engineer")
                .permanentAddress("No 45 Temple Road Colombo 03")
                .phone("0771234567")
                .gramaNiladhariDivision("514B Kollupitiya")
                .divisionalSecretariat("Colombo")
                .photoFile(photo)
                .birthCertFile(birthCert)
                .build();

        NicApplication application = nicService.submitApplication(dto, citizen);

        assertNotNull(application.getId());
        assertTrue(application.getReferenceNumber().startsWith("NIC-"));
        assertEquals(ApplicationStatus.PENDING_PAYMENT, application.getStatus());
        assertEquals(PaymentStatus.PENDING, application.getPaymentStatus());
        assertEquals(500.0, application.getFeeAmount());

        // Verify MS SQL binary document storage
        List<ApplicationDocument> docs = fileStorageService.getDocumentsByApplication(application.getId(), ApplicationType.NIC);
        assertEquals(2, docs.size());

        // 3. Checkout & Process Payment
        PaymentRequestDto payReq = paymentService.prepareCheckout(ApplicationType.NIC, application.getId(), citizen);
        assertEquals(500.0, payReq.getAmount());
        assertEquals(application.getReferenceNumber(), payReq.getApplicationReference());

        payReq.setCardHolderName("Y THUJIKOSHAN");
        payReq.setCardNumber("4242 4242 4242 4242");
        payReq.setExpiryMonth("12");
        payReq.setExpiryYear("28");
        payReq.setCvv("123");

        PaymentTransaction txn = paymentService.processPayment(payReq, citizen);
        assertNotNull(txn.getId());
        assertTrue(txn.getTransactionId().startsWith("TXN-"));
        assertEquals(PaymentStatus.PAID, txn.getPaymentStatus());
        assertEquals("4242", txn.getCardLastFour());

        // Verify status moved to UNDER_REVIEW
        NicApplication updatedApp = nicService.getApplicationById(application.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updatedApp.getPaymentStatus());
        assertEquals(ApplicationStatus.UNDER_REVIEW, updatedApp.getStatus());

        // 4. Officer Verification Queue
        List<VerificationItemDto> queue = verificationService.getQueue("NIC", "ALL", null);
        assertFalse(queue.isEmpty());
        assertTrue(queue.stream().anyMatch(i -> i.getReferenceNumber().equals(application.getReferenceNumber())));

        // 5. Officer Review & Decision (Approve)
        User officer = userRepository.findByEmail("officer@nidis.gov.lk")
                .orElseThrow(() -> new IllegalStateException("Officer not found"));

        VerificationDecisionDto decision = VerificationDecisionDto.builder()
                .applicationType(ApplicationType.NIC)
                .applicationId(application.getId())
                .action("APPROVE")
                .officerComment("Bio-data verified with Birth Certificate. Approved.")
                .build();

        verificationService.processDecision(decision, officer);

        NicApplication approvedApp = nicService.getApplicationById(application.getId()).orElseThrow();
        assertEquals(ApplicationStatus.APPROVED, approvedApp.getStatus());
        assertEquals("Bio-data verified with Birth Certificate. Approved.", approvedApp.getOfficerComment());

        // 5b. Officer Verifies Payment and Ships Document (PAYMENT_VERIFY)
        VerificationDecisionDto shipDecision = VerificationDecisionDto.builder()
                .applicationType(ApplicationType.NIC)
                .applicationId(application.getId())
                .action("PAYMENT_VERIFY")
                .officerComment("Payment verified and smart NIC card dispatched to citizen address.")
                .build();

        verificationService.processDecision(shipDecision, officer);

        NicApplication shippedApp = nicService.getApplicationById(application.getId()).orElseThrow();
        assertEquals(ApplicationStatus.SHIPPED, shippedApp.getStatus());
        assertNotNull(shippedApp.getIssuedNicNumber(), "Issued NIC number must be automatically generated");
        assertEquals(12, shippedApp.getIssuedNicNumber().length(), "Generated NIC must be 12 digits");
        assertTrue(shippedApp.getIssuedNicNumber().startsWith("2001"), "NIC must start with applicant birth year 2001");

        // Verify citizen can use this issued NIC number to look up and prefill for renewal or lost claims
        var prefillRecord = nicService.prefillBioData(shippedApp.getIssuedNicNumber());
        assertTrue(prefillRecord.isPresent(), "Citizen must be able to lookup bio-data using issued NIC number");
        assertEquals("YOGATHAS THUJIKOSHAN", prefillRecord.get().getFullName());

        // 6. Super Admin User & Audit Logs
        User admin = userRepository.findByEmail("admin@nidis.gov.lk").orElseThrow();
        List<AuditLog> auditLogs = auditLogService.getRecentLogs();
        assertFalse(auditLogs.isEmpty());
        assertTrue(auditLogs.stream().anyMatch(l -> l.getEntityId().equals(application.getReferenceNumber())));

        // Test Admin User Management
        adminUserService.toggleUserStatus(citizen.getId(), admin.getEmail());
        User toggledUser = userRepository.findById(citizen.getId()).orElseThrow();
        assertFalse(toggledUser.getIsActive());

        adminUserService.toggleUserStatus(citizen.getId(), admin.getEmail());
        User restoredUser = userRepository.findById(citizen.getId()).orElseThrow();
        assertTrue(restoredUser.getIsActive());
    }
}
