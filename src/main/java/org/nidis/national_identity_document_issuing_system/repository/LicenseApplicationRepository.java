package org.nidis.national_identity_document_issuing_system.repository;

import java.util.List;
import java.util.Optional;

import org.nidis.national_identity_document_issuing_system.model.LicenseApplication;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationCategory;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LicenseApplicationRepository extends JpaRepository<LicenseApplication, Long> {
    List<LicenseApplication> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<LicenseApplication> findByReferenceNumber(String referenceNumber);
    Optional<LicenseApplication> findTopByExistingLicenseNumberOrderByCreatedAtDesc(String existingLicenseNumber);
    List<LicenseApplication> findByStatusInOrderByCreatedAtAsc(List<ApplicationStatus> statuses);
    Optional<LicenseApplication> findFirstByUserIdAndCategoryAndIsDraftTrue(Long userId, ApplicationCategory category);
    long countByUserId(Long userId);
    long countByUserIdAndStatus(Long userId, ApplicationStatus status);
}

