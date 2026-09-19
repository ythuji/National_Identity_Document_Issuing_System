package org.nidis.national_identity_document_issuing_system.repository;

import java.util.List;
import java.util.Optional;

import org.nidis.national_identity_document_issuing_system.model.ApplicationDocument;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, Long> {
    List<ApplicationDocument> findByApplicationIdAndApplicationType(Long applicationId, ApplicationType applicationType);
    Optional<ApplicationDocument> findByApplicationIdAndApplicationTypeAndDocumentType(Long applicationId, ApplicationType applicationType, String documentType);
    void deleteByApplicationIdAndApplicationType(Long applicationId, ApplicationType applicationType);
}

