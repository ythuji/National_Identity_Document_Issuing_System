package org.nidis.national_identity_document_issuing_system.dto;

import java.time.LocalDate;

import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationCategory;
import org.nidis.national_identity_document_issuing_system.validation.OverEighteen;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LicenseApplicationDto {

    private Long id;

    @NotNull(message = "Application category is required")
    private ApplicationCategory category;

    private String existingLicenseNumber;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @OverEighteen
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Residential address is required")
    private String address;

    @NotBlank(message = "Contact phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must contain exactly 10 digits")
    private String phone;

    @NotBlank(message = "Vehicle class is required")
    private String vehicleClass;

    private Boolean medicalCertRequired = false;

    private String policeReportRef;

    // Document attachments
    private MultipartFile photoFile;
    private MultipartFile idProofFile;
    private MultipartFile medicalCertFile;
    private MultipartFile policeReportFile;
}

