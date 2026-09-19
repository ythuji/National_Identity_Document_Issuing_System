package org.nidis.national_identity_document_issuing_system.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nidis.national_identity_document_issuing_system.model.Role;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.nidis.national_identity_document_issuing_system.model.enums.RoleName;
import org.nidis.national_identity_document_issuing_system.repository.RoleRepository;
import org.nidis.national_identity_document_issuing_system.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> searchUsers(String query) {
        if (query == null || query.isBlank()) {
            return userRepository.findAll();
        }
        return userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query.trim(), query.trim());
    }

    @Transactional
    public void toggleUserStatus(Long userId, String adminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        boolean newStatus = !Boolean.TRUE.equals(user.getIsActive());
        user.setIsActive(newStatus);
        userRepository.save(user);

        String action = newStatus ? "USER_ACTIVATED" : "USER_SUSPENDED";
        auditLogService.recordLog(adminEmail, action, "USER", String.valueOf(userId),
                "Admin " + adminEmail + " changed active status of user " + user.getEmail() + " to " + newStatus, "127.0.0.1");

        log.info("User {} active status set to {} by admin {}", user.getEmail(), newStatus, adminEmail);
    }

    @Transactional
    public void updateUserRoles(Long userId, List<String> roleNames, String adminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        Set<Role> roles = new HashSet<>();
        for (String rName : roleNames) {
            try {
                RoleName enumRole = RoleName.valueOf(rName);
                Role role = roleRepository.findByRoleName(enumRole)
                        .orElseGet(() -> roleRepository.save(Role.builder().roleName(enumRole).build()));
                roles.add(role);
            } catch (Exception ex) {
                log.warn("Invalid role specified: {}", rName);
            }
        }

        if (roles.isEmpty()) {
            // Default back to APPLICANT if none specified
            roleRepository.findByRoleName(RoleName.APPLICANT).ifPresent(roles::add);
        }

        user.setRoles(roles);
        userRepository.save(user);

        auditLogService.recordLog(adminEmail, "ROLES_UPDATED", "USER", String.valueOf(userId),
                "Admin " + adminEmail + " updated roles for user " + user.getEmail() + " to: " + roleNames, "127.0.0.1");

        log.info("Roles updated for user {} by admin {}", user.getEmail(), adminEmail);
    }
}
