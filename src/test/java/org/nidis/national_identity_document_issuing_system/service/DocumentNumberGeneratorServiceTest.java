package org.nidis.national_identity_document_issuing_system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.nidis.national_identity_document_issuing_system.repository.LicenseApplicationRepository;
import org.nidis.national_identity_document_issuing_system.repository.NicApplicationRepository;
import org.nidis.national_identity_document_issuing_system.repository.PassportApplicationRepository;
import org.nidis.national_identity_document_issuing_system.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentNumberGeneratorServiceTest {

    @Mock
    private NicApplicationRepository nicRepository;

    @Mock
    private LicenseApplicationRepository licenseRepository;

    @Mock
    private PassportApplicationRepository passportRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DocumentNumberGeneratorService generatorService;

    @BeforeEach
    void setUp() {
        when(nicRepository.existsByIssuedNicNumber(anyString())).thenReturn(false);
        when(userRepository.existsByNicNumber(anyString())).thenReturn(false);
        when(licenseRepository.existsByIssuedLicenseNumber(anyString())).thenReturn(false);
        when(passportRepository.existsByIssuedPassportNumber(anyString())).thenReturn(false);
    }

    @Test
    void testGenerateNicNumberMale() {
        LocalDate dob = LocalDate.of(1998, 5, 10); // 130th day of year
        String nic = generatorService.generateNicNumber(dob, "Male");

        assertNotNull(nic);
        assertEquals(12, nic.length(), "NIC number must be exactly 12 digits");
        assertTrue(nic.matches("\\d{12}"), "NIC number must contain only numeric digits");
        assertTrue(nic.startsWith("1998"), "NIC must start with birth year");

        // Day of year for 1998-05-10 is 130
        String dayDigits = nic.substring(4, 7);
        assertEquals(String.format("%03d", dob.getDayOfYear()), dayDigits, "Male day of year must match directly");
    }

    @Test
    void testGenerateNicNumberFemale() {
        LocalDate dob = LocalDate.of(2002, 3, 1); // 60th day of year
        String nic = generatorService.generateNicNumber(dob, "Female");

        assertNotNull(nic);
        assertEquals(12, nic.length(), "NIC number must be exactly 12 digits");
        assertTrue(nic.matches("\\d{12}"), "NIC number must contain only numeric digits");
        assertTrue(nic.startsWith("2002"), "NIC must start with birth year");

        // Day of year for Female has +500 offset
        int expectedDayOfYearWithFemaleOffset = dob.getDayOfYear() + 500;
        String dayDigits = nic.substring(4, 7);
        assertEquals(String.format("%03d", expectedDayOfYearWithFemaleOffset), dayDigits,
                "Female day of year must have 500 added to day of year");
    }

    @Test
    void testGenerateLicenseNumber() {
        String licenseNumber = generatorService.generateLicenseNumber();

        assertNotNull(licenseNumber);
        assertEquals(8, licenseNumber.length(), "Driving License must be 8 characters long (B + 7 digits)");
        assertTrue(licenseNumber.startsWith("B"), "Driving License must start with prefix 'B'");
        assertTrue(licenseNumber.substring(1).matches("\\d{7}"), "Remaining 7 characters must be numeric digits");
    }

    @Test
    void testGeneratePassportNumber() {
        String passportNumber = generatorService.generatePassportNumber();

        assertNotNull(passportNumber);
        assertEquals(8, passportNumber.length(), "Passport number must be 8 characters long (N + 7 digits)");
        assertTrue(passportNumber.startsWith("N"), "Passport number must start with prefix 'N'");
        assertTrue(passportNumber.substring(1).matches("\\d{7}"), "Remaining 7 characters must be numeric digits");
    }
}

