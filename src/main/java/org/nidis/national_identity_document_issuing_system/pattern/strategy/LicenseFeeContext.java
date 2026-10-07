package org.nidis.national_identity_document_issuing_system.pattern.strategy;

import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationCategory;
import org.springframework.stereotype.Component;

/**
 * Strategy Context — License Fee Context.
 *
 * Acts as the context class in the Strategy Pattern. Selects the correct
 * {@link LicenseFeeStrategy} at runtime based on the {@link ApplicationCategory}.
 *
 * Usage in PaymentService:
 * <pre>
 *   LicenseFeeStrategy strategy = licenseFeeContext.getStrategy(category);
 *   double fee = strategy.calculateFee();
 * </pre>
 *
 * Design Pattern: Strategy (GoF Behavioral)
 * Where used: Injected into PaymentService to replace hard-coded fee switch.
 * Benefit: Adding a new category only requires a new strategy class — no
 *          changes needed in PaymentService or anywhere else.
 */
@Component
public class LicenseFeeContext {

    /**
     * Returns the appropriate fee strategy for the given license category.
     *
     * @param category the ApplicationCategory (NEW, RENEWAL, LOST)
     * @return the matching LicenseFeeStrategy implementation
     * @throws IllegalArgumentException if the category is null or unrecognized
     */
    public LicenseFeeStrategy getStrategy(ApplicationCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("Application category must not be null when determining license fee.");
        }
        return switch (category) {
            case NEW      -> new NewLicenseFeeStrategy();
            case RENEWAL  -> new RenewalLicenseFeeStrategy();
            case LOST     -> new LostLicenseFeeStrategy();
        };
    }
}
