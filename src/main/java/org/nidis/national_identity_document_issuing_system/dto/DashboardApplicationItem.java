package org.nidis.national_identity_document_issuing_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationStatus;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardApplicationItem {
    private Long id;
    private String referenceNumber;
    private String serviceName; // "Driver's License", "Passport Services", "National Identity Card"
    private ApplicationType serviceType; // LICENSE, PASSPORT, NIC
    private String category;
    private LocalDateTime createdAt;
    private ApplicationStatus status;
    private String viewUrl;
    private String documentNumber;
}
