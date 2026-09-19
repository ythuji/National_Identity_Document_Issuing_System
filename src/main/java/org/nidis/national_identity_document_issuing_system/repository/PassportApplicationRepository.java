package org.nidis.national_identity_document_issuing_system.repository;

import java.util.List;
import java.util.Optional;

import org.nidis.national_identity_document_issuing_system.model.PassportApplication;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationCategory;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PassportApplicationRepository extends JpaRepository<PassportApplication, Long> {
    List<PassportApplication> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<PassportApplication> findByReferenceNumber(String referenceNumber);
    Optional<PassportApplication> findTopByExistingPassportNumberOrderByCreatedAtDesc(String existingPassportNumber);
    List<PassportApplication> findByStatusInOrderByCreatedAtAsc(List<ApplicationStatus> statuses);
    Optional<PassportApplication> findFirstByUserIdAndCategoryAndIsDraftTrue(Long userId, ApplicationCategory category);
    long countByUserId(Long userId);
    long countByUserIdAndStatus(Long userId, ApplicationStatus status);
}

