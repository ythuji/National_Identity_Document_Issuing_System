package org.nidis.national_identity_document_issuing_system.dto;

import java.time.LocalDateTime;

import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationCategory;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationStatus;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;
import org.nidis.national_identity_document_issuing_system.model.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationItemDto {
    private Long id;
    private ApplicationType serviceType;
    private String referenceNumber;
    private String applicantName;
    private String applicantEmail;
    private String applicantNic;
    private ApplicationCategory category;
    private ApplicationStatus status;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
    private String reviewUrl;
}

