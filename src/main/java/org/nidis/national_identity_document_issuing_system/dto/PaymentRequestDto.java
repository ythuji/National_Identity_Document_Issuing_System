package org.nidis.national_identity_document_issuing_system.dto;

import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDto {

    @NotNull
    private ApplicationType applicationType;

    @NotNull
    private Long applicationId;

    @NotBlank
    private String applicationReference;

    private String serviceName;

    @NotNull
    private Double amount;

    @NotBlank(message = "Cardholder name is required")
    private String cardHolderName;

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^[0-9\\s]{13,19}$", message = "Please enter a valid 16-digit card number")
    private String cardNumber;

    @NotBlank(message = "Expiry MM is required")
    @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Expiry month must be between 01 and 12")
    private String expiryMonth;

    @NotBlank(message = "Expiry YY is required")
    @Pattern(regexp = "^[0-9]{2}$", message = "Expiry year must be 2 digits (e.g. 27)")
    private String expiryYear;

    @NotBlank(message = "CVV is required")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVV must be 3 or 4 digits")
    private String cvv;

    @Builder.Default
    private String paymentMethod = "CARD";
}

