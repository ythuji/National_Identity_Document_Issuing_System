package org.nidis.national_identity_document_issuing_system.pattern.strategy;

/**
 * Concrete Strategy: Driver's License Renewal Fee.
 *
 * Calculates the processing fee for renewing an existing valid license.
 * Fee: LKR 1,500.00 (reduced renewal processing charge).
 */
public class RenewalLicenseFeeStrategy implements LicenseFeeStrategy {

    private static final double RENEWAL_LICENSE_FEE = 1500.00;

    @Override
    public double calculateFee() {
        return RENEWAL_LICENSE_FEE;
    }

    @Override
    public String getDescription() {
        return "Driver's License Renewal Processing Fee — LKR 1,500.00";
    }
}
