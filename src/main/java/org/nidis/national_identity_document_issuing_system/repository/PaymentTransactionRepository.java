package org.nidis.national_identity_document_issuing_system.repository;

import java.util.List;
import java.util.Optional;

import org.nidis.national_identity_document_issuing_system.model.PaymentTransaction;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByTransactionId(String transactionId);
    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<PaymentTransaction> findByApplicationIdAndApplicationType(Long applicationId, ApplicationType applicationType);
    Optional<PaymentTransaction> findFirstByApplicationIdAndApplicationTypeOrderByCreatedAtDesc(Long applicationId, ApplicationType applicationType);
}

