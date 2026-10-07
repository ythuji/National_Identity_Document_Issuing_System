package org.nidis.national_identity_document_issuing_system.pattern.observer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nidis.national_identity_document_issuing_system.model.PaymentTransaction;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.nidis.national_identity_document_issuing_system.service.NotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Observer Pattern — License Payment Email/Notification Listener.
 *
 * Listens for {@link LicensePaymentCompletedEvent} and sends an in-system
 * notification to the applicant confirming that their license payment was
 * successfully processed.
 *
 * Design Pattern: Observer (GoF Behavioral, implemented via Spring @EventListener)
 * Benefit: PaymentService has NO dependency on NotificationService —
 *          this listener is the only coupling point, fully independent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LicenseEmailNotificationListener {

    private final NotificationService notificationService;

    /**
     * Triggered automatically by Spring when a {@link LicensePaymentCompletedEvent}
     * is published. Sends the payment confirmation notification to the user.
     */
    @EventListener
    public void onLicensePaymentCompleted(LicensePaymentCompletedEvent event) {
        PaymentTransaction txn = event.getPaymentTransaction();
        User user = event.getUser();

        String subject = "Driver's License Payment Confirmed — " + txn.getApplicationReference();
        String message = String.format(
                "Dear %s,%n%n" +
                "Your payment of LKR %.2f for your Driver's License application (%s) has been " +
                "successfully processed.%n%n" +
                "Transaction ID : %s%n" +
                "Payment Method : %s%n%n" +
                "Your application is now queued for officer verification. You will receive a " +
                "further notification once the review is complete.%n%n" +
                "National Identity Document Issuing System (NIDIS)",
                user.getFullName(),
                txn.getAmount(),
                txn.getApplicationReference(),
                txn.getTransactionId(),
                txn.getPaymentMethod()
        );

        notificationService.sendNotification(user.getId(), subject, message);
        log.info("[Observer] LicenseEmailNotificationListener: payment notification sent to user {} for txn {}",
                user.getEmail(), txn.getTransactionId());
    }
}
