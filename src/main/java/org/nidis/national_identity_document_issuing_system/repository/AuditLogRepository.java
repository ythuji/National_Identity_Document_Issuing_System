package org.nidis.national_identity_document_issuing_system.repository;

import java.util.List;

import org.nidis.national_identity_document_issuing_system.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByTimestampDesc();
    List<AuditLog> findTop100ByOrderByTimestampDesc();
    List<AuditLog> findByActorEmailOrderByTimestampDesc(String actorEmail);
    List<AuditLog> findByActionOrderByTimestampDesc(String action);
}

