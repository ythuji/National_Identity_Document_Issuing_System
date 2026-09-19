package org.nidis.national_identity_document_issuing_system.service;

import java.util.List;

import org.nidis.national_identity_document_issuing_system.model.AuditLog;
import org.nidis.national_identity_document_issuing_system.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void recordLog(String actorEmail, String action, String entityName, String entityId, String details, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .actorEmail(actorEmail != null ? actorEmail : "SYSTEM")
                    .action(action)
                    .entityName(entityName)
                    .entityId(entityId)
                    .details(details)
                    .ipAddress(ipAddress != null ? ipAddress : "127.0.0.1")
                    .build();
            auditLogRepository.save(auditLog);
            log.info("Audit log recorded: [{} - {}] by {}", action, entityId, actorEmail);
        } catch (Exception ex) {
            log.error("Failed to record audit log: ", ex);
        }
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByTimestampDesc();
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }
}

