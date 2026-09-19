package org.nidis.national_identity_document_issuing_system.model;

import java.time.LocalDateTime;

import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "application_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationType applicationType;

    @Column(nullable = false, length = 200)
    private String documentName;

    @Column(nullable = false, length = 50)
    private String documentType; // PHOTO, MEDICAL_CERT, POLICE_REPORT, ID_PROOF, BIRTH_CERT

    @Column(nullable = false, length = 100)
    private String contentType;

    @Lob
    @Column(name = "file_data", columnDefinition = "VARBINARY(MAX)", nullable = false)
    private byte[] fileData;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
}

