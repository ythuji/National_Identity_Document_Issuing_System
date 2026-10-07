package org.nidis.national_identity_document_issuing_system.pattern.observer;

import org.nidis.national_identity_document_issuing_system.model.PaymentTransaction;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.springframework.context.ApplicationEvent;

/**
 * Observer Pattern (Behavioral) — License Payment Completed Event.
 *
 * This event is published by {@link org.nidis.national_identity_document_issuing_system.service.PaymentService}
 * after a successful payment for a Driver's License application is saved to the database.
 *
 * Observers (listeners) of this event:
 *   - {@link LicenseEmailNotificationListener} — sends in-system notification to the user
 *   - {@link LicenseAuditLogListener}          — records an audit trail entry
 *
 * Design Pattern: Observer / Event-Driven (GoF Behavioral, Spring ApplicationEvent)
 * Where used: Published from PaymentService; consumed by two independent listeners.
 * Benefit: PaymentService is completely decoupled from notification and audit logic.
 *          Adding a new action on payment success (e.g., SMS, analytics) requires
 *          only a new @EventListener — no changes to PaymentService whatsoever.
 */
public class LicensePaymentCompletedEvent extends ApplicationEvent {

    private final PaymentTransaction paymentTransaction;
    private final User user;

    public LicensePaymentCompletedEvent(Object source, PaymentTransaction paymentTransaction, User user) {
        super(source);
        this.paymentTransaction = paymentTransaction;
        this.user = user;
    }

    public PaymentTransaction getPaymentTransaction() {
        return paymentTransaction;
    }

    public User getUser() {
        return user;
    }
}
