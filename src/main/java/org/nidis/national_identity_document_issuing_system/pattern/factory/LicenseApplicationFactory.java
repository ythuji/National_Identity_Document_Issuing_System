package org.nidis.national_identity_document_issuing_system.pattern.factory;

import org.nidis.national_identity_document_issuing_system.dto.LicenseApplicationDto;
import org.nidis.national_identity_document_issuing_system.model.LicenseApplication;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationCategory;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationStatus;

import java.time.LocalDateTime;

/**
 * Factory Pattern (Creational) — License Application Factory.
 *
 * Centralises all category-specific creation and pre-condition logic for
 * {@link LicenseApplication} objects. Instead of scattering if/else
 * construction logic inside LicenseService, this factory decides how to
 * build and initialise each object depending on the ApplicationCategory.
 *
 * Design Pattern: Factory Method (GoF Creational)
 * Where used: Called from LicenseService.submitApplication()
 * Benefit: Single Responsibility — LicenseService only orchestrates;
 *          object construction is fully owned here.  Adding a new category
 *          means adding a new private method inside this factory only.
 */
public class LicenseApplicationFactory {

    // Private constructor — this is a utility factory; not to be instantiated.
    private LicenseApplicationFactory() {}

    /**
     * Creates a new {@link LicenseApplication} entity from a DTO and the
     * authenticated user, populating category-specific fields and setting
     * mandatory defaults.
     *
     * @param dto             the submitted form DTO
     * @param user            the authenticated applicant
     * @param referenceNumber the pre-generated unique reference number
     * @return a fully initialised, unsaved {@link LicenseApplication}
     */
    public static LicenseApplication create(LicenseApplicationDto dto, User user, String referenceNumber) {
        return switch (dto.getCategory()) {
            case NEW      -> createNew(dto, user, referenceNumber);
            case RENEWAL  -> createRenewal(dto, user, referenceNumber);
            case LOST     -> createLost(dto, user, referenceNumber);
        };
    }

    // ------------------------------------------------------------------
    // Category-specific factory methods
    // ------------------------------------------------------------------

    /**
     * Builds a NEW license application.
     * No existing license number or police report is required.
     */
    private static LicenseApplication createNew(LicenseApplicationDto dto, User user, String referenceNumber) {
        LicenseApplication app = baseApplication(dto, user, referenceNumber);
        app.setCategory(ApplicationCategory.NEW);
        // NEW applications: no prior license number, no police report
        app.setExistingLicenseNumber(null);
        app.setPoliceReportRef(null);
        return app;
    }

    /**
     * Builds a RENEWAL license application.
     * The applicant must supply their existing license number.
     */
    private static LicenseApplication createRenewal(LicenseApplicationDto dto, User user, String referenceNumber) {
        if (dto.getExistingLicenseNumber() == null || dto.getExistingLicenseNumber().isBlank()) {
            throw new IllegalArgumentException("Existing driving license number is required for license renewal.");
        }
        LicenseApplication app = baseApplication(dto, user, referenceNumber);
        app.setCategory(ApplicationCategory.RENEWAL);
        app.setExistingLicenseNumber(dto.getExistingLicenseNumber().trim());
        app.setPoliceReportRef(null);
        return app;
    }

    /**
     * Builds a LOST license application.
     * The applicant must supply a police report reference.
     */
    private static LicenseApplication createLost(LicenseApplicationDto dto, User user, String referenceNumber) {
        if (dto.getPoliceReportRef() == null || dto.getPoliceReportRef().isBlank()) {
            throw new IllegalArgumentException("Police report reference number is required for lost license replacements.");
        }
        LicenseApplication app = baseApplication(dto, user, referenceNumber);
        app.setCategory(ApplicationCategory.LOST);
        app.setExistingLicenseNumber(
                dto.getExistingLicenseNumber() != null ? dto.getExistingLicenseNumber().trim() : null);
        app.setPoliceReportRef(dto.getPoliceReportRef().trim());
        return app;
    }

    // ------------------------------------------------------------------
    // Shared base builder — sets fields common to all categories
    // ------------------------------------------------------------------

    private static LicenseApplication baseApplication(LicenseApplicationDto dto, User user, String referenceNumber) {
        LicenseApplication app = new LicenseApplication();
        app.setUser(user);
        app.setReferenceNumber(referenceNumber);
        app.setFullName(dto.getFullName().trim());
        app.setDateOfBirth(dto.getDateOfBirth());
        app.setAddress(dto.getAddress().trim());
        app.setPhone(dto.getPhone().trim());
        app.setVehicleClass(dto.getVehicleClass().trim());
        app.setMedicalCertRequired(Boolean.TRUE.equals(dto.getMedicalCertRequired()));
        app.setStatus(ApplicationStatus.SUBMITTED);
        app.setIsDraft(false);
        app.setCreatedAt(LocalDateTime.now());
        app.setUpdatedAt(LocalDateTime.now());
        return app;
    }
}
