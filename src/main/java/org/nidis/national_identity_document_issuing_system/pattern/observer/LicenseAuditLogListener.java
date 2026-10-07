package org.nidis.national_identity_document_issuing_system.pattern.observer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nidis.national_identity_document_issuing_system.model.PaymentTransaction;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.nidis.national_identity_document_issuing_system.service.AuditLogService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Observer Pattern — License Payment Audit Log Listener.
 *
 * Listens for {@link LicensePaymentCompletedEvent} and writes a permanent
 * audit trail record for every successful license payment transaction.
 *
 * Design Pattern: Observer (GoF Behavioral, implemented via Spring @EventListener)
 * Benefit: Audit concerns are completely separated from payment processing.
 *          The audit log is always written independently, even if notification fails.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LicenseAuditLogListener {

    private final AuditLogService auditLogService;

    /**
     * Triggered automatically by Spring when a {@link LicensePaymentCompletedEvent}
     * is published. Records payment details in the audit log.
     */
    @EventListener
    public void onLicensePaymentCompleted(LicensePaymentCompletedEvent event) {
        PaymentTransaction txn = event.getPaymentTransaction();
        User user = event.getUser();

        String details = String.format(
                "License payment completed: amount=LKR %.2f, method=%s, ref=%s, txnId=%s",
                txn.getAmount(),
                txn.getPaymentMethod(),
                txn.getApplicationReference(),
                txn.getTransactionId()
        );

        auditLogService.recordLog(
                user.getEmail(),
                "LICENSE_PAYMENT_COMPLETED",
                "PaymentTransaction",
                String.valueOf(txn.getId()),
                details,
                null
        );

        log.info("[Observer] LicenseAuditLogListener: audit entry recorded for txn {} by user {}",
                txn.getTransactionId(), user.getEmail());
    }
}
