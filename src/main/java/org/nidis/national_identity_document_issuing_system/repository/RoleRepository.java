package org.nidis.national_identity_document_issuing_system.repository;

import org.nidis.national_identity_document_issuing_system.model.Role;
import org.nidis.national_identity_document_issuing_system.model.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(RoleName roleName);
    boolean existsByRoleName(RoleName roleName);
}
