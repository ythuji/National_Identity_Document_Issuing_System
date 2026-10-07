package org.nidis.national_identity_document_issuing_system.pattern.strategy;

/**
 * Strategy Pattern (Behavioral) — License Fee Calculation Strategy Interface.
 *
 * Defines a common contract for calculating driving license processing fees
 * across all three application categories: NEW, RENEWAL, and LOST.
 *
 * Design Pattern: Strategy (GoF Behavioral)
 * Benefit: Eliminates hard-coded if/switch fee blocks. Each strategy is
 * independently testable and new categories can be added without modifying
 * existing code (Open/Closed Principle).
 */
public interface LicenseFeeStrategy {

    /**
     * Calculate the total processing fee for a license application.
     * @return the fee amount in LKR (Sri Lankan Rupees)
     */
    double calculateFee();

    /**
     * Returns the name/description of this fee strategy for display / audit.
     */
    String getDescription();
}
