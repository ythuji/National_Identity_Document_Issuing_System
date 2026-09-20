package org.nidis.national_identity_document_issuing_system.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationCategory;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "passport_applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassportApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationCategory category;

    @Column(nullable = false, unique = true, length = 30)
    private String referenceNumber;

    @Column(length = 20)
    private String existingPassportNumber;

    @Column(name = "issued_passport_number", length = 20)
    private String issuedPassportNumber;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 100)
    private String placeOfBirth;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String nationality = "Sri Lankan";

    @Column(nullable = false, length = 20)
    private String gender;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 100)
    private String profession;

    @Column(nullable = false)
    @Builder.Default
    private Boolean dualCitizenship = false;

    @Column(length = 50)
    private String policeReportRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDraft = false;

    @Column(length = 500)
    private String officerComment;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

