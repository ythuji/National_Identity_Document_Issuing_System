package org.nidis.national_identity_document_issuing_system.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.nidis.national_identity_document_issuing_system.model.OtpToken;
import org.nidis.national_identity_document_issuing_system.model.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findTopByEmailAndTypeAndIsUsedFalseOrderByCreatedAtDesc(String email, OtpType type);
    List<OtpToken> findByEmailAndTypeAndIsUsedFalse(String email, OtpType type);
    void deleteByExpiresAtBefore(LocalDateTime now);
}

