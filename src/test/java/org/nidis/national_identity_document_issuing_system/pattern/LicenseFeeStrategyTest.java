package org.nidis.national_identity_document_issuing_system.pattern;

import org.junit.jupiter.api.Test;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationCategory;
import org.nidis.national_identity_document_issuing_system.pattern.strategy.LicenseFeeContext;
import org.nidis.national_identity_document_issuing_system.pattern.strategy.LicenseFeeStrategy;
import org.nidis.national_identity_document_issuing_system.pattern.strategy.LostLicenseFeeStrategy;
import org.nidis.national_identity_document_issuing_system.pattern.strategy.NewLicenseFeeStrategy;
import org.nidis.national_identity_document_issuing_system.pattern.strategy.RenewalLicenseFeeStrategy;

import static org.junit.jupiter.api.Assertions.*;

public class LicenseFeeStrategyTest {

    private final LicenseFeeContext context = new LicenseFeeContext();

    @Test
    void testNewLicenseFeeStrategy() {
        LicenseFeeStrategy strategy = context.getStrategy(ApplicationCategory.NEW);
        assertTrue(strategy instanceof NewLicenseFeeStrategy);
        assertEquals(2500.00, strategy.calculateFee());
        assertTrue(strategy.getDescription().contains("2,500.00"));
    }

    @Test
    void testRenewalLicenseFeeStrategy() {
        LicenseFeeStrategy strategy = context.getStrategy(ApplicationCategory.RENEWAL);
        assertTrue(strategy instanceof RenewalLicenseFeeStrategy);
        assertEquals(1500.00, strategy.calculateFee());
        assertTrue(strategy.getDescription().contains("1,500.00"));
    }

    @Test
    void testLostLicenseFeeStrategy() {
        LicenseFeeStrategy strategy = context.getStrategy(ApplicationCategory.LOST);
        assertTrue(strategy instanceof LostLicenseFeeStrategy);
        assertEquals(2000.00, strategy.calculateFee());
        assertTrue(strategy.getDescription().contains("2,000.00"));
    }

    @Test
    void testNullCategoryThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> context.getStrategy(null));
    }
}
