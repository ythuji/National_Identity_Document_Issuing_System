package org.nidis.national_identity_document_issuing_system.model;

import jakarta.persistence.*;
import lombok.*;
import org.nidis.national_identity_document_issuing_system.model.enums.RoleName;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private RoleName roleName;
}
