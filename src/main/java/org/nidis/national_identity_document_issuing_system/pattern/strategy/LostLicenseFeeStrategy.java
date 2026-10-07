package org.nidis.national_identity_document_issuing_system.pattern.strategy;

/**
 * Concrete Strategy: Lost Driver's License Replacement Fee.
 *
 * Calculates the processing fee for replacing a lost or stolen license.
 * Fee: LKR 2,000.00 (includes administration + duplicate-issuance surcharge).
 */
public class LostLicenseFeeStrategy implements LicenseFeeStrategy {

    private static final double LOST_LICENSE_FEE = 2000.00;

    @Override
    public double calculateFee() {
        return LOST_LICENSE_FEE;
    }

    @Override
    public String getDescription() {
        return "Lost Driver's License Replacement Fee — LKR 2,000.00";
    }
}
