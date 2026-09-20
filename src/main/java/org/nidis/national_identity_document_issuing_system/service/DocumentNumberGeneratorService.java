package org.nidis.national_identity_document_issuing_system.service;

import java.security.SecureRandom;
import java.time.LocalDate;

import org.nidis.national_identity_document_issuing_system.repository.LicenseApplicationRepository;
import org.nidis.national_identity_document_issuing_system.repository.NicApplicationRepository;
import org.nidis.national_identity_document_issuing_system.repository.PassportApplicationRepository;
import org.nidis.national_identity_document_issuing_system.repository.UserRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for generating authentic Sri Lankan format document numbers:
 * - NIC: 12-digit format (YYYY + DDD (+500 for female) + 0 + Serial + Check)
 * - Driving License: Department of Motor Traffic format (B + 7 digits)
 * - Passport: Department of Immigration & Emigration format (N + 7 digits)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentNumberGeneratorService {

    private final NicApplicationRepository nicRepository;
    private final LicenseApplicationRepository licenseRepository;
    private final PassportApplicationRepository passportRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a unique 12-digit Sri Lankan National Identity Card (NIC) number.
     * Format: YYYY (4-digit year) + DDD (3-digit day of year, +500 for females) + 0 (1-digit) + NNN (3-digit serial) + C (1-digit check)
     */
    public String generateNicNumber(LocalDate dateOfBirth, String gender) {
        int year = (dateOfBirth != null) ? dateOfBirth.getYear() : (LocalDate.now().getYear() - 20);
        int dayOfYear = (dateOfBirth != null) ? dateOfBirth.getDayOfYear() : (secureRandom.nextInt(365) + 1);

        if (gender != null && gender.trim().equalsIgnoreCase("Female")) {
            dayOfYear += 500;
        }

        String nic;
        int attempts = 0;
        do {
            int serial = secureRandom.nextInt(900) + 100; // 3-digit serial (100-999)
            int checkDigit = secureRandom.nextInt(10);     // 1-digit check digit (0-9)
            nic = String.format("%04d%03d0%03d%d", year, dayOfYear, serial, checkDigit);
            attempts++;
            if (attempts > 1000) {
                log.warn("Exceeded maximum attempts generating unique NIC; using generated value: {}", nic);
                break;
            }
        } while (nicRepository.existsByIssuedNicNumber(nic) || userRepository.existsByNicNumber(nic));

        log.info("Generated official Sri Lankan NIC number: {} (DOB: {}, Gender: {})", nic, dateOfBirth, gender);
        return nic;
    }

    /**
     * Generates a unique Sri Lankan Driving License number (DMT format).
     * Format: 'B' followed by 7 numeric digits (e.g. B8294105).
     */
    public String generateLicenseNumber() {
        String licenseNumber;
        int attempts = 0;
        do {
            int serial = secureRandom.nextInt(9000000) + 1000000; // 7 digits
            licenseNumber = "B" + serial;
            attempts++;
            if (attempts > 1000) {
                log.warn("Exceeded maximum attempts generating unique License number; using: {}", licenseNumber);
                break;
            }
        } while (licenseRepository.existsByIssuedLicenseNumber(licenseNumber));

        log.info("Generated official Sri Lankan Driving License number: {}", licenseNumber);
        return licenseNumber;
    }

    /**
     * Generates a unique Sri Lankan Passport number (DIE format).
     * Format: 'N' followed by 7 numeric digits (e.g. N7482910).
     */
    public String generatePassportNumber() {
        String passportNumber;
        int attempts = 0;
        do {
            int serial = secureRandom.nextInt(9000000) + 1000000; // 7 digits
            passportNumber = "N" + serial;
            attempts++;
            if (attempts > 1000) {
                log.warn("Exceeded maximum attempts generating unique Passport number; using: {}", passportNumber);
                break;
            }
        } while (passportRepository.existsByIssuedPassportNumber(passportNumber));

        log.info("Generated official Sri Lankan Passport number: {}", passportNumber);
        return passportNumber;
    }
}

