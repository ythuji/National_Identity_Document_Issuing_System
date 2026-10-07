package org.nidis.national_identity_document_issuing_system.pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nidis.national_identity_document_issuing_system.model.PaymentTransaction;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;
import org.nidis.national_identity_document_issuing_system.model.enums.PaymentStatus;
import org.nidis.national_identity_document_issuing_system.pattern.observer.LicenseAuditLogListener;
import org.nidis.national_identity_document_issuing_system.pattern.observer.LicenseEmailNotificationListener;
import org.nidis.national_identity_document_issuing_system.pattern.observer.LicensePaymentCompletedEvent;
import org.nidis.national_identity_document_issuing_system.service.AuditLogService;
import org.nidis.national_identity_document_issuing_system.service.NotificationService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LicenseObserverPatternTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private LicenseEmailNotificationListener emailNotificationListener;

    @InjectMocks
    private LicenseAuditLogListener auditLogListener;

    @Test
    void testEmailNotificationListenerReceivesEvent() {
        User user = User.builder()
                .id(42L)
                .email("driver@example.com")
                .fullName("Sunil Perera")
                .build();

        PaymentTransaction txn = PaymentTransaction.builder()
                .id(999L)
                .transactionId("TXN-20261007-8888")
                .applicationReference("LIC-20261007-1111")
                .applicationType(ApplicationType.LICENSE)
                .amount(2500.0)
                .paymentMethod("Visa/Mastercard")
                .paymentStatus(PaymentStatus.PAID)
                .build();

        LicensePaymentCompletedEvent event = new LicensePaymentCompletedEvent(this, txn, user);

        // Execute observer
        emailNotificationListener.onLicensePaymentCompleted(event);

        // Verify notification was dispatched
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(notificationService, times(1)).sendNotification(
                eq(42L),
                subjectCaptor.capture(),
                messageCaptor.capture()
        );

        assertTrue(subjectCaptor.getValue().contains("LIC-20261007-1111"));
        assertTrue(messageCaptor.getValue().contains("2500.00"));
        assertTrue(messageCaptor.getValue().contains("TXN-20261007-8888"));
    }

    @Test
    void testAuditLogListenerReceivesEvent() {
        User user = User.builder()
                .id(42L)
                .email("driver@example.com")
                .fullName("Sunil Perera")
                .build();

        PaymentTransaction txn = PaymentTransaction.builder()
                .id(999L)
                .transactionId("TXN-20261007-8888")
                .applicationReference("LIC-20261007-1111")
                .applicationType(ApplicationType.LICENSE)
                .amount(2500.0)
                .paymentMethod("Visa/Mastercard")
                .paymentStatus(PaymentStatus.PAID)
                .build();

        LicensePaymentCompletedEvent event = new LicensePaymentCompletedEvent(this, txn, user);

        // Execute observer
        auditLogListener.onLicensePaymentCompleted(event);

        // Verify audit log record was created
        verify(auditLogService, times(1)).recordLog(
                eq("driver@example.com"),
                eq("LICENSE_PAYMENT_COMPLETED"),
                eq("PaymentTransaction"),
                eq("999"),
                contains("TXN-20261007-8888"),
                isNull()
        );
    }
}
