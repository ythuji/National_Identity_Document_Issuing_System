package org.nidis.national_identity_document_issuing_system.pattern.strategy;

/**
 * Concrete Strategy: New Driver's License Fee.
 *
 * Calculates the processing fee for a brand-new license application.
 * Fee: LKR 2,500.00 (base road-test + registration charge).
 */
public class NewLicenseFeeStrategy implements LicenseFeeStrategy {

    private static final double NEW_LICENSE_FEE = 2500.00;

    @Override
    public double calculateFee() {
        return NEW_LICENSE_FEE;
    }

    @Override
    public String getDescription() {
        return "New Driver's License Registration Fee — LKR 2,500.00";
    }
}
