package org.nidis.national_identity_document_issuing_system.dto;

import java.time.LocalDate;

import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationCategory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NicApplicationDto {

    private Long id;

    private ApplicationCategory category;

    private String existingNicNumber;

    @NotBlank(message = "Full Name is required")
    @Size(max = 100, message = "Full Name must not exceed 100 characters")
    private String fullName;

    @NotNull(message = "Date of Birth is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotBlank(message = "Civil Status is required")
    private String civilStatus;

    private String occupation;

    @NotBlank(message = "Permanent Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String permanentAddress;

    @NotBlank(message = "Phone Number is required")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @NotBlank(message = "Grama Niladhari Division is required")
    private String gramaNiladhariDivision;

    @NotBlank(message = "Divisional Secretariat is required")
    private String divisionalSecretariat;

    private String policeReportRef;

    private Double feeAmount;

    // File attachments
    private MultipartFile photoFile;
    private MultipartFile birthCertFile;
    private MultipartFile policeReportFile;
    private MultipartFile addressProofFile;

    // Existing document IDs for review
    private Long photoDocId;
    private Long birthCertDocId;
    private Long policeReportDocId;
    private Long addressProofDocId;
}

