package org.nidis.national_identity_document_issuing_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nidis.national_identity_document_issuing_system.dto.RegistrationDto;
import org.nidis.national_identity_document_issuing_system.model.OtpToken;
import org.nidis.national_identity_document_issuing_system.model.enums.OtpType;
import org.nidis.national_identity_document_issuing_system.repository.OtpTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void generateAndSendRegistrationOtp(RegistrationDto dto) {
        String cleanEmail = dto.getEmail().toLowerCase().trim();
        List<OtpToken> existingTokens = otpTokenRepository.findByEmailAndTypeAndIsUsedFalse(cleanEmail, OtpType.EMAIL_VERIFY);
        for (OtpToken token : existingTokens) {
            token.setIsUsed(true);
        }
        otpTokenRepository.saveAll(existingTokens);

        String code = generate6DigitCode();
        String hashedPassword = passwordEncoder.encode(dto.getPassword());

        OtpToken otpToken = OtpToken.builder()
                .email(cleanEmail)
                .otpCode(code)
                .type(OtpType.EMAIL_VERIFY)
                .fullName(dto.getFullName().trim())
                .nicNumber(dto.getNicNumber().trim())
                .phone(dto.getPhone().trim())
                .passwordHash(hashedPassword)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .isUsed(false)
                .attemptCount(0)
                .build();

        otpTokenRepository.save(otpToken);
        emailService.sendOtpEmail(cleanEmail, code, dto.getFullName().trim());
    }

    @Transactional
    public void generateAndSendPasswordResetOtp(String email) {
        String cleanEmail = email.toLowerCase().trim();
        List<OtpToken> existingTokens = otpTokenRepository.findByEmailAndTypeAndIsUsedFalse(cleanEmail, OtpType.PASSWORD_RESET);
        for (OtpToken token : existingTokens) {
            token.setIsUsed(true);
        }
        otpTokenRepository.saveAll(existingTokens);

        String code = generate6DigitCode();

        OtpToken otpToken = OtpToken.builder()
                .email(cleanEmail)
                .otpCode(code)
                .type(OtpType.PASSWORD_RESET)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .isUsed(false)
                .attemptCount(0)
                .build();

        otpTokenRepository.save(otpToken);
        emailService.sendPasswordResetEmail(cleanEmail, code);
    }

    @Transactional
    public void generateAndSendLoginOtp(String email, String fullName) {
        String cleanEmail = email.toLowerCase().trim();
        List<OtpToken> existingTokens = otpTokenRepository.findByEmailAndTypeAndIsUsedFalse(cleanEmail, OtpType.LOGIN_VERIFY);
        for (OtpToken token : existingTokens) {
            token.setIsUsed(true);
        }
        otpTokenRepository.saveAll(existingTokens);

        String code = generate6DigitCode();

        OtpToken otpToken = OtpToken.builder()
                .email(cleanEmail)
                .otpCode(code)
                .type(OtpType.LOGIN_VERIFY)
                .fullName(fullName)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .isUsed(false)
                .attemptCount(0)
                .build();

        otpTokenRepository.save(otpToken);
        emailService.sendLoginOtpEmail(cleanEmail, code, fullName);
    }

    @Transactional
    public OtpToken verifyOtp(String email, String otpCode, OtpType type) {
        String cleanEmail = email.toLowerCase().trim();
        Optional<OtpToken> optToken = otpTokenRepository.findTopByEmailAndTypeAndIsUsedFalseOrderByCreatedAtDesc(cleanEmail, type);

        if (optToken.isEmpty()) {
            throw new IllegalArgumentException("No active verification code found for this email. Please request a new code.");
        }

        OtpToken token = optToken.get();

        if (token.getAttemptCount() >= 3) {
            throw new IllegalStateException("Maximum verification attempts exceeded. Please request a new OTP code.");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification code has expired. Please request a new code.");
        }

        if (!token.getOtpCode().equals(otpCode.trim())) {
            token.setAttemptCount(token.getAttemptCount() + 1);
            otpTokenRepository.save(token);
            int remaining = 3 - token.getAttemptCount();
            throw new IllegalArgumentException("Invalid verification code. " + remaining + " attempt(s) remaining.");
        }

        token.setIsUsed(true);
        return otpTokenRepository.save(token);
    }

    private String generate6DigitCode() {
        int number = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(number);
    }
}

