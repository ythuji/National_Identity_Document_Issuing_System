package org.nidis.national_identity_document_issuing_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nidis.national_identity_document_issuing_system.model.OtpToken;
import org.nidis.national_identity_document_issuing_system.model.Role;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.nidis.national_identity_document_issuing_system.model.enums.RoleName;
import org.nidis.national_identity_document_issuing_system.repository.RoleRepository;
import org.nidis.national_identity_document_issuing_system.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email.toLowerCase().trim());
    }

    public boolean existsByNicNumber(String nicNumber) {
        return userRepository.existsByNicNumber(nicNumber.trim());
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim());
    }

    @Transactional
    public User createUserFromVerifiedOtp(OtpToken token) {
        if (existsByEmail(token.getEmail())) {
            throw new IllegalStateException("An account with this email address already exists.");
        }

        Role applicantRole = roleRepository.findByRoleName(RoleName.APPLICANT)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.APPLICANT).build()));

        User user = User.builder()
                .fullName(token.getFullName())
                .email(token.getEmail().toLowerCase().trim())
                .nicNumber(token.getNicNumber())
                .phone(token.getPhone())
                .passwordHash(token.getPasswordHash())
                .isActive(true)
                .isEmailVerified(true)
                .roles(new HashSet<>())
                .build();

        user.getRoles().add(applicantRole);
        User savedUser = userRepository.save(user);
        log.info("New user successfully registered and activated: {} (ID: {})", savedUser.getEmail(), savedUser.getId());
        return savedUser;
    }

    @Transactional
    public void updatePassword(String email, String rawPassword) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
        log.info("Password successfully updated for user: {}", email);
    }
}

