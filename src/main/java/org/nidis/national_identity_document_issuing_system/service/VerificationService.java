package org.nidis.national_identity_document_issuing_system.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.nidis.national_identity_document_issuing_system.dto.VerificationDecisionDto;
import org.nidis.national_identity_document_issuing_system.dto.VerificationItemDto;
import org.nidis.national_identity_document_issuing_system.model.ApplicationDocument;
import org.nidis.national_identity_document_issuing_system.model.LicenseApplication;
import org.nidis.national_identity_document_issuing_system.model.NicApplication;
import org.nidis.national_identity_document_issuing_system.model.PassportApplication;
import org.nidis.national_identity_document_issuing_system.model.PaymentTransaction;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationStatus;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;
import org.nidis.national_identity_document_issuing_system.model.enums.PaymentStatus;
import org.nidis.national_identity_document_issuing_system.repository.LicenseApplicationRepository;
import org.nidis.national_identity_document_issuing_system.repository.NicApplicationRepository;
import org.nidis.national_identity_document_issuing_system.repository.PassportApplicationRepository;
import org.nidis.national_identity_document_issuing_system.repository.PaymentTransactionRepository;
import org.nidis.national_identity_document_issuing_system.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VerificationService {

    private final LicenseApplicationRepository licenseRepository;
    private final PassportApplicationRepository passportRepository;
    private final NicApplicationRepository nicRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final UserRepository userRepository;
    private final DocumentNumberGeneratorService documentNumberGenerator;
    private final EmailService emailService;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public List<VerificationItemDto> getQueue(String serviceFilter, String statusFilter, String search) {
        List<VerificationItemDto> items = new ArrayList<>();

        // 1. License Applications
        if (serviceFilter == null || serviceFilter.equalsIgnoreCase("ALL") || serviceFilter.equalsIgnoreCase("LICENSE")) {
            List<LicenseApplication> licenseApps = licenseRepository.findAll();
            for (LicenseApplication app : licenseApps) {
                if (!Boolean.TRUE.equals(app.getIsDraft())) {
                    PaymentStatus pStatus = paymentRepository.findFirstByApplicationIdAndApplicationTypeOrderByCreatedAtDesc(app.getId(), ApplicationType.LICENSE)
                            .map(PaymentTransaction::getPaymentStatus).orElse(PaymentStatus.PAID);

                    items.add(VerificationItemDto.builder()
                            .id(app.getId())
                            .serviceType(ApplicationType.LICENSE)
                            .referenceNumber(app.getReferenceNumber())
                            .applicantName(app.getFullName())
                            .applicantEmail(app.getUser().getEmail())
                            .applicantNic(app.getUser().getNicNumber())
                            .category(app.getCategory())
                            .status(app.getStatus())
                            .paymentStatus(pStatus)
                            .createdAt(app.getCreatedAt())
                            .reviewUrl("/verification/review/LICENSE/" + app.getId())
                            .build());
                }
            }
        }

        // 2. Passport Applications
        if (serviceFilter == null || serviceFilter.equalsIgnoreCase("ALL") || serviceFilter.equalsIgnoreCase("PASSPORT")) {
            List<PassportApplication> passportApps = passportRepository.findAll();
            for (PassportApplication app : passportApps) {
                if (!Boolean.TRUE.equals(app.getIsDraft())) {
                    PaymentStatus pStatus = paymentRepository.findFirstByApplicationIdAndApplicationTypeOrderByCreatedAtDesc(app.getId(), ApplicationType.PASSPORT)
                            .map(PaymentTransaction::getPaymentStatus).orElse(PaymentStatus.PAID);

                    items.add(VerificationItemDto.builder()
                            .id(app.getId())
                            .serviceType(ApplicationType.PASSPORT)
                            .referenceNumber(app.getReferenceNumber())
                            .applicantName(app.getFullName())
                            .applicantEmail(app.getUser().getEmail())
                            .applicantNic(app.getUser().getNicNumber())
                            .category(app.getCategory())
                            .status(app.getStatus())
                            .paymentStatus(pStatus)
                            .createdAt(app.getCreatedAt())
                            .reviewUrl("/verification/review/PASSPORT/" + app.getId())
                            .build());
                }
            }
        }

        // 3. NIC Applications
        if (serviceFilter == null || serviceFilter.equalsIgnoreCase("ALL") || serviceFilter.equalsIgnoreCase("NIC")) {
            List<NicApplication> nicApps = nicRepository.findAll();
            for (NicApplication app : nicApps) {
                if (!Boolean.TRUE.equals(app.getIsDraft())) {
                    items.add(VerificationItemDto.builder()
                            .id(app.getId())
                            .serviceType(ApplicationType.NIC)
                            .referenceNumber(app.getReferenceNumber())
                            .applicantName(app.getFullName())
                            .applicantEmail(app.getUser().getEmail())
                            .applicantNic(app.getExistingNicNumber() != null ? app.getExistingNicNumber() : app.getUser().getNicNumber())
                            .category(app.getCategory())
                            .status(app.getStatus())
                            .paymentStatus(app.getPaymentStatus())
                            .createdAt(app.getCreatedAt())
                            .reviewUrl("/verification/review/NIC/" + app.getId())
                            .build());
                }
            }
        }

        // Filter by status if specified and not ALL
        if (statusFilter != null && !statusFilter.equalsIgnoreCase("ALL") && !statusFilter.isBlank()) {
            items = items.stream()
                    .filter(i -> i.getStatus().name().equalsIgnoreCase(statusFilter))
                    .collect(Collectors.toList());
        }

        // Filter by keyword search if provided
        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            items = items.stream().filter(i ->
                    (i.getReferenceNumber() != null && i.getReferenceNumber().toLowerCase().contains(q)) ||
                    (i.getApplicantName() != null && i.getApplicantName().toLowerCase().contains(q)) ||
                    (i.getApplicantEmail() != null && i.getApplicantEmail().toLowerCase().contains(q)) ||
                    (i.getApplicantNic() != null && i.getApplicantNic().toLowerCase().contains(q))
            ).collect(Collectors.toList());
        }

        // Sort newest first
        items.sort(Comparator.comparing(VerificationItemDto::getCreatedAt).reversed());
        return items;
    }

    public List<ApplicationDocument> getAttachedDocuments(ApplicationType type, Long id) {
        return fileStorageService.getDocumentsByApplication(id, type);
    }

    public PaymentStatus getPaymentStatus(ApplicationType type, Long id) {
        return paymentRepository.findFirstByApplicationIdAndApplicationTypeOrderByCreatedAtDesc(id, type)
                .map(PaymentTransaction::getPaymentStatus)
                .orElse(PaymentStatus.PENDING);
    }

    @Transactional
    public void markReceived(ApplicationType type, Long id, User applicant) {
        String referenceNumber;
        switch (type) {
            case LICENSE -> {
                LicenseApplication app = licenseRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("License application not found."));
                validateApplicantOwnership(app.getUser(), applicant, app.getStatus());
                app.setStatus(ApplicationStatus.RECEIVED);
                licenseRepository.save(app);
                referenceNumber = app.getReferenceNumber();
            }
            case PASSPORT -> {
                PassportApplication app = passportRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Passport application not found."));
                validateApplicantOwnership(app.getUser(), applicant, app.getStatus());
                app.setStatus(ApplicationStatus.RECEIVED);
                passportRepository.save(app);
                referenceNumber = app.getReferenceNumber();
            }
            case NIC -> {
                NicApplication app = nicRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("NIC application not found."));
                validateApplicantOwnership(app.getUser(), applicant, app.getStatus());
                app.setStatus(ApplicationStatus.RECEIVED);
                nicRepository.save(app);
                referenceNumber = app.getReferenceNumber();
            }
            default -> throw new IllegalArgumentException("Unsupported application type.");
        }

        notificationService.sendNotification(applicant.getId(),
                "Application Received: " + referenceNumber,
                "You confirmed receipt of your " + type + " card for application " + referenceNumber + ".");
        auditLogService.recordLog(applicant.getEmail(), "APPLICATION_RECEIVED", type.name(), referenceNumber,
                "Applicant confirmed receipt of the issued card.", "127.0.0.1");
    }

    private void validateApplicantOwnership(User owner, User applicant, ApplicationStatus currentStatus) {
        if (owner == null || !owner.getId().equals(applicant.getId())) {
            throw new IllegalStateException("You are not allowed to update this application.");
        }
        if (currentStatus != ApplicationStatus.SHIPPED) {
            throw new IllegalStateException("The application must be shipped before it can be marked as received.");
        }
    }

    @Transactional
    public void processDecision(VerificationDecisionDto dto, User officer) {
        // PAYMENT_VERIFY directly marks as SHIPPED (one-step: verify payment + ship)
        ApplicationStatus newStatus = switch (dto.getAction().toUpperCase()) {
            case "APPROVE" -> ApplicationStatus.APPROVED;
            case "REJECT" -> ApplicationStatus.REJECTED;
            case "CORRECTION" -> ApplicationStatus.PENDING_CORRECTION;
            case "PAYMENT_VERIFY" -> ApplicationStatus.SHIPPED;
            default -> throw new IllegalArgumentException("Unknown decision action: " + dto.getAction());
        };

        String refNo = "";
        Long userId = null;
        String citizenEmail = "";
        String citizenName = "";
        String issuedDocNumber = null;
        String docTypeDisplayName = "";

        switch (dto.getApplicationType()) {
            case LICENSE -> {
                LicenseApplication app = licenseRepository.findById(dto.getApplicationId())
                        .orElseThrow(() -> new IllegalArgumentException("License application not found."));
                validateWorkflowTransition(app.getStatus(), newStatus,
                    paymentRepository.findFirstByApplicationIdAndApplicationTypeOrderByCreatedAtDesc(app.getId(), ApplicationType.LICENSE)
                        .map(PaymentTransaction::getPaymentStatus).orElse(PaymentStatus.PENDING));

                docTypeDisplayName = "Driving License";
                refNo = app.getReferenceNumber();
                userId = app.getUser().getId();
                citizenEmail = app.getUser().getEmail();
                citizenName = app.getFullName();

                if (newStatus == ApplicationStatus.SHIPPED) {
                    issuedDocNumber = app.getIssuedLicenseNumber();
                    if (issuedDocNumber == null || issuedDocNumber.isBlank()) {
                        if (app.getExistingLicenseNumber() != null && !app.getExistingLicenseNumber().isBlank()) {
                            issuedDocNumber = app.getExistingLicenseNumber().trim().toUpperCase();
                        } else {
                            issuedDocNumber = documentNumberGenerator.generateLicenseNumber();
                        }
                        app.setIssuedLicenseNumber(issuedDocNumber);
                    }
                }

                app.setStatus(newStatus);
                app.setOfficerComment(dto.getOfficerComment());
                licenseRepository.save(app);
            }
            case PASSPORT -> {
                PassportApplication app = passportRepository.findById(dto.getApplicationId())
                        .orElseThrow(() -> new IllegalArgumentException("Passport application not found."));
                validateWorkflowTransition(app.getStatus(), newStatus,
                    paymentRepository.findFirstByApplicationIdAndApplicationTypeOrderByCreatedAtDesc(app.getId(), ApplicationType.PASSPORT)
                        .map(PaymentTransaction::getPaymentStatus).orElse(PaymentStatus.PENDING));

                docTypeDisplayName = "Passport";
                refNo = app.getReferenceNumber();
                userId = app.getUser().getId();
                citizenEmail = app.getUser().getEmail();
                citizenName = app.getFullName();

                if (newStatus == ApplicationStatus.SHIPPED) {
                    issuedDocNumber = app.getIssuedPassportNumber();
                    if (issuedDocNumber == null || issuedDocNumber.isBlank()) {
                        if (app.getExistingPassportNumber() != null && !app.getExistingPassportNumber().isBlank()) {
                            issuedDocNumber = app.getExistingPassportNumber().trim().toUpperCase();
                        } else {
                            issuedDocNumber = documentNumberGenerator.generatePassportNumber();
                        }
                        app.setIssuedPassportNumber(issuedDocNumber);
                    }
                }

                app.setStatus(newStatus);
                app.setOfficerComment(dto.getOfficerComment());
                passportRepository.save(app);
            }
            case NIC -> {
                NicApplication app = nicRepository.findById(dto.getApplicationId())
                        .orElseThrow(() -> new IllegalArgumentException("NIC application not found."));
                validateWorkflowTransition(app.getStatus(), newStatus, app.getPaymentStatus());

                docTypeDisplayName = "National Identity Card (NIC)";
                refNo = app.getReferenceNumber();
                userId = app.getUser().getId();
                citizenEmail = app.getUser().getEmail();
                citizenName = app.getFullName();

                if (newStatus == ApplicationStatus.SHIPPED) {
                    issuedDocNumber = app.getIssuedNicNumber();
                    if (issuedDocNumber == null || issuedDocNumber.isBlank()) {
                        if (app.getExistingNicNumber() != null && !app.getExistingNicNumber().isBlank()) {
                            issuedDocNumber = app.getExistingNicNumber().trim().toUpperCase();
                        } else {
                            issuedDocNumber = documentNumberGenerator.generateNicNumber(app.getDateOfBirth(), app.getGender());
                        }
                        app.setIssuedNicNumber(issuedDocNumber);
                    }

                    // Sync to user profile if user doesn't have an NIC registered yet
                    User citizen = app.getUser();
                    if (citizen != null && (citizen.getNicNumber() == null || citizen.getNicNumber().isBlank())) {
                        citizen.setNicNumber(issuedDocNumber);
                        userRepository.save(citizen);
                    }
                }

                app.setStatus(newStatus);
                app.setOfficerComment(dto.getOfficerComment());
                nicRepository.save(app);
            }
        }

        // Send citizen notification
        String statusSubject = switch (newStatus) {
            case APPROVED -> "Application Approved - Payment Required: " + refNo;
            case SHIPPED -> "Payment Verified & " + docTypeDisplayName + " Shipped (" + (issuedDocNumber != null ? issuedDocNumber : refNo) + ")";
            case REJECTED -> "Application Rejected: " + refNo;
            case PENDING_CORRECTION -> "Action Required / Correction Requested: " + refNo;
            default -> "Application Status Updated: " + refNo;
        };

        String messageBody = switch (newStatus) {
            case APPROVED -> "Your " + docTypeDisplayName + " application (" + refNo
                + ") has been approved. Please pay by card from your application details page: /payment/checkout/"
                + dto.getApplicationType() + "/" + dto.getApplicationId() + ".";
            case SHIPPED -> "Your payment for " + refNo + " has been verified by the officer. Your official "
                + docTypeDisplayName + " (Number: " + issuedDocNumber + ") has been issued and dispatched. "
                + "Please securely record this number for any future renewals or lost replacement applications. "
                + "Once you receive the physical document, please mark it as Received from your dashboard.";
            default -> "Your " + docTypeDisplayName + " application (" + refNo + ") status has been updated to: "
                + newStatus + ".\n\nOfficer Comments: " + (dto.getOfficerComment() != null ? dto.getOfficerComment() : "None");
        };

        notificationService.sendNotification(userId, statusSubject, messageBody);

        // Send official issuance email to citizen's email address
        if (newStatus == ApplicationStatus.SHIPPED && issuedDocNumber != null && citizenEmail != null && !citizenEmail.isBlank()) {
            emailService.sendDocumentIssuedEmail(citizenEmail, citizenName, docTypeDisplayName, issuedDocNumber, refNo);
        }

        // Record Audit Log
        auditLogService.recordLog(officer.getEmail(),
                "APPLICATION_" + newStatus.name(),
                dto.getApplicationType().name(),
                refNo,
                "Officer " + officer.getFullName() + " set status to " + newStatus
                + (issuedDocNumber != null ? " with official number: " + issuedDocNumber : "")
                + ". Remarks: " + dto.getOfficerComment(),
                "127.0.0.1");

        log.info("Application [{}] status changed to {} by officer {} (Issued Number: {})",
                refNo, newStatus, officer.getEmail(), issuedDocNumber);
    }

    private void validateWorkflowTransition(ApplicationStatus currentStatus,
                                            ApplicationStatus newStatus,
                                            PaymentStatus paymentStatus) {
        if (newStatus == ApplicationStatus.SHIPPED) {
            if (currentStatus != ApplicationStatus.APPROVED) {
                throw new IllegalStateException("Payment can only be verified and the document shipped after the application is approved. Current status: " + currentStatus);
            }
            if (paymentStatus != PaymentStatus.PAID) {
                throw new IllegalStateException("The citizen must complete payment before the officer can verify and dispatch the document.");
            }
        }
    }
}
