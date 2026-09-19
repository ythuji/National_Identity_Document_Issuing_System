package org.nidis.national_identity_document_issuing_system.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String actorEmail;

    @Column(nullable = false, length = 50)
    private String action; // e.g. APPLICATION_APPROVED, APPLICATION_REJECTED, CORRECTION_REQUESTED, ROLE_ASSIGNED, USER_TOGGLED, PAYMENT_COMPLETED

    @Column(nullable = false, length = 50)
    private String entityName; // e.g. LICENSE, PASSPORT, NIC, USER, PAYMENT

    @Column(length = 50)
    private String entityId; // Reference Number or User ID

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String details;

    @Column(length = 50)
    private String ipAddress;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}

