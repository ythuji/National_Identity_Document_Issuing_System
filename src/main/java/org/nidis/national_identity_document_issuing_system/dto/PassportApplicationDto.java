package org.nidis.national_identity_document_issuing_system.dto;

import java.time.LocalDate;

import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationCategory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PassportApplicationDto {

    private Long id;

    @NotNull(message = "Application category is required")
    private ApplicationCategory category;

    private String existingPassportNumber;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Place of birth is required")
    private String placeOfBirth;

    @NotBlank(message = "Nationality is required")
    private String nationality = "Sri Lankan";

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotBlank(message = "Permanent address is required")
    private String address;

    @NotBlank(message = "Contact phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must contain exactly 10 digits")
    private String phone;

    private String profession;

    private Boolean dualCitizenship = false;

    private String policeReportRef;

    // Attachments
    private MultipartFile photoFile;
    private MultipartFile nicCopyFile;
    private MultipartFile birthCertFile;
    private MultipartFile policeReportFile;
}

