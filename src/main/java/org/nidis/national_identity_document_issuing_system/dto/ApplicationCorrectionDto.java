package org.nidis.national_identity_document_issuing_system.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationCorrectionDto {

    private Long id;

    private String phone;

    private String address;

    private String occupation;

    private String civilStatus;

    private String applicantNote;

    private MultipartFile photoFile;

    private MultipartFile birthCertFile;

    private MultipartFile addressProofFile;

    private MultipartFile policeReportFile;

    private MultipartFile medicalCertFile;
}

