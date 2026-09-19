package org.nidis.national_identity_document_issuing_system.dto;

import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationDecisionDto {

    @NotNull
    private ApplicationType applicationType;

    @NotNull
    private Long applicationId;

    @NotBlank
    private String action; // APPROVE, REJECT, CORRECTION, PAYMENT_VERIFY, SHIP

    private String officerComment;
}

