package org.nidis.national_identity_document_issuing_system.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.nidis.national_identity_document_issuing_system.dto.PaymentRequestDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentTransactionRepository paymentRepository;
    private final LicenseApplicationRepository licenseRepository;
    private final PassportApplicationRepository passportRepository;
    private final NicApplicationRepository nicRepository;
    private final NotificationService notificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    public PaymentRequestDto prepareCheckout(ApplicationType type, Long applicationId, User user) {
        PaymentRequestDto dto = new PaymentRequestDto();
        dto.setApplicationType(type);
        dto.setApplicationId(applicationId);

        switch (type) {
            case LICENSE: {
                LicenseApplication app = licenseRepository.findById(applicationId)
                        .orElseThrow(() -> new IllegalArgumentException("License application not found."));
                if (!app.getUser().getId().equals(user.getId())) {
                    throw new IllegalStateException("Unauthorized access.");
                }
                requireApprovedForPayment(app.getStatus());
                dto.setApplicationReference(app.getReferenceNumber());
                dto.setServiceName("Sri Lanka Driving License (" + app.getCategory() + ")");
                dto.setAmount(switch (app.getCategory()) {
                    case NEW -> 2500.0;
                    case RENEWAL -> 1500.0;
                    case LOST -> 2000.0;
                });
                break;
            }
            case PASSPORT: {
                PassportApplication app = passportRepository.findById(applicationId)
                        .orElseThrow(() -> new IllegalArgumentException("Passport application not found."));
                if (!app.getUser().getId().equals(user.getId())) {
                    throw new IllegalStateException("Unauthorized access.");
                }
                requireApprovedForPayment(app.getStatus());
                dto.setApplicationReference(app.getReferenceNumber());
                dto.setServiceName("Sri Lankan Passport Service (" + app.getCategory() + ")");
                dto.setAmount(switch (app.getCategory()) {
                    case NEW -> 5000.0;
                    case RENEWAL -> 5000.0;
                    case LOST -> 10000.0;
                });
                break;
            }
            case NIC: {
                NicApplication app = nicRepository.findById(applicationId)
                        .orElseThrow(() -> new IllegalArgumentException("NIC application not found."));
                if (!app.getUser().getId().equals(user.getId())) {
                    throw new IllegalStateException("Unauthorized access.");
                }
                requireApprovedOrInitialPayment(app.getStatus());
                dto.setApplicationReference(app.getReferenceNumber());
                dto.setServiceName("Department for Registration of Persons — Smart NIC (" + app.getCategory() + ")");
                dto.setAmount(app.getFeeAmount() != null ? app.getFeeAmount() : 500.0);
                break;
            }
        }
        return dto;
    }

    @Transactional
    public PaymentTransaction processPayment(PaymentRequestDto dto, User user) {
        prepareCheckout(dto.getApplicationType(), dto.getApplicationId(), user);
        String cleanCard = dto.getCardNumber().replaceAll("\\s+", "");
        String lastFour = cleanCard.length() >= 4 ? cleanCard.substring(cleanCard.length() - 4) : "0000";

        // Simulated test card gateway validation:
        // If CVV is 000 -> simulate transaction failure
        if ("000".equals(dto.getCvv())) {
            PaymentTransaction failedTxn = PaymentTransaction.builder()
                    .transactionId(generateTransactionId())
                    .user(user)
                    .applicationType(dto.getApplicationType())
                    .applicationId(dto.getApplicationId())
                    .applicationReference(dto.getApplicationReference())
                    .amount(dto.getAmount())
                    .paymentMethod(dto.getPaymentMethod())
                    .cardLastFour(lastFour)
                    .paymentStatus(PaymentStatus.FAILED)
                    .failureReason("Simulated Gateway Error: Card declined by issuing bank (Invalid CVV/Authorization Failed).")
                    .build();
            paymentRepository.save(failedTxn);
            throw new IllegalArgumentException("Payment was declined by issuing bank. Please verify card details or try a different card.");
        }

        String txnId = generateTransactionId();
        PaymentTransaction txn = PaymentTransaction.builder()
                .transactionId(txnId)
                .user(user)
                .applicationType(dto.getApplicationType())
                .applicationId(dto.getApplicationId())
                .applicationReference(dto.getApplicationReference())
                .amount(dto.getAmount())
                .paymentMethod(dto.getPaymentMethod())
                .cardLastFour(lastFour)
                .paymentStatus(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .build();

        PaymentTransaction savedTxn = paymentRepository.save(txn);

        // Update application statuses
        switch (dto.getApplicationType()) {
            case NIC: {
                nicRepository.findById(dto.getApplicationId()).ifPresent(app -> {
                    app.setPaymentStatus(PaymentStatus.PAID);
                    if (app.getStatus() == ApplicationStatus.PENDING_PAYMENT || app.getStatus() == ApplicationStatus.SUBMITTED) {
                        app.setStatus(ApplicationStatus.UNDER_REVIEW);
                    }
                    nicRepository.save(app);
                });
                break;
            }
            case LICENSE: {
                licenseRepository.findById(dto.getApplicationId()).ifPresent(app -> {
                    if (app.getStatus() == ApplicationStatus.SUBMITTED) {
                        app.setStatus(ApplicationStatus.UNDER_REVIEW);
                    }
                    licenseRepository.save(app);
                });
                break;
            }
            case PASSPORT: {
                passportRepository.findById(dto.getApplicationId()).ifPresent(app -> {
                    if (app.getStatus() == ApplicationStatus.SUBMITTED) {
                        app.setStatus(ApplicationStatus.UNDER_REVIEW);
                    }
                    passportRepository.save(app);
                });
                break;
            }
        }

        // Send payment confirmation notification
        notificationService.sendNotification(user.getId(),
                "Payment Successful: " + txn.getTransactionId(),
                "Your payment of LKR " + String.format("%.2f", txn.getAmount()) + " for "
                        + txn.getApplicationReference() + " (" + txn.getApplicationType() + ") was successfully processed.");

        log.info("Payment transaction [{}] processed successfully for user: {} (Amount: LKR {})",
                savedTxn.getTransactionId(), user.getEmail(), savedTxn.getAmount());

        return savedTxn;
    }

    private void requireApprovedForPayment(ApplicationStatus status) {
        if (status != ApplicationStatus.APPROVED) {
            throw new IllegalStateException("Payment is available only after an officer approves the application.");
        }
    }

    private void requireApprovedOrInitialPayment(ApplicationStatus status) {
        if (status != ApplicationStatus.APPROVED && status != ApplicationStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Payment is not available for this application state.");
        }
    }

    public Optional<PaymentTransaction> getTransactionByTxnId(String txnId) {
        return paymentRepository.findByTransactionId(txnId);
    }

    public List<PaymentTransaction> getUserTransactions(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<PaymentTransaction> getLatestPaymentForApp(Long applicationId, ApplicationType type) {
        return paymentRepository.findFirstByApplicationIdAndApplicationTypeOrderByCreatedAtDesc(applicationId, type);
    }

    public String generateTransactionId() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = secureRandom.nextInt(90000) + 10000;
        return "TXN-" + dateStr + "-" + rand;
    }
}

