package org.nidis.national_identity_document_issuing_system.repository;

import java.util.List;
import java.util.Optional;

import org.nidis.national_identity_document_issuing_system.model.NicApplication;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationCategory;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NicApplicationRepository extends JpaRepository<NicApplication, Long> {
    List<NicApplication> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<NicApplication> findByUserIdAndCategoryAndIsDraftTrue(Long userId, ApplicationCategory category);
    Optional<NicApplication> findByReferenceNumber(String referenceNumber);
    Optional<NicApplication> findFirstByExistingNicNumberOrderByCreatedAtDesc(String existingNicNumber);
    List<NicApplication> findByStatusOrderByCreatedAtDesc(ApplicationStatus status);
    List<NicApplication> findByStatusInOrderByCreatedAtDesc(List<ApplicationStatus> statuses);
    long countByStatus(ApplicationStatus status);
}

