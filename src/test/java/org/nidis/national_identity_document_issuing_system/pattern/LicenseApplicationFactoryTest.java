package org.nidis.national_identity_document_issuing_system.pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nidis.national_identity_document_issuing_system.dto.LicenseApplicationDto;
import org.nidis.national_identity_document_issuing_system.model.LicenseApplication;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationCategory;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationStatus;
import org.nidis.national_identity_document_issuing_system.pattern.factory.LicenseApplicationFactory;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class LicenseApplicationFactoryTest {

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(100L)
                .email("thujikoshan@example.com")
                .fullName("Thujikoshan Yogathas")
                .build();
    }

    private LicenseApplicationDto createBaseDto() {
        LicenseApplicationDto dto = new LicenseApplicationDto();
        dto.setFullName("Thujikoshan Yogathas");
        dto.setDateOfBirth(LocalDate.of(2000, 5, 15));
        dto.setAddress("123 Galle Road, Colombo 03");
        dto.setPhone("0771234567");
        dto.setVehicleClass("B (Car/Van)");
        return dto;
    }

    @Test
    void testCreateNewLicenseApplication() {
        LicenseApplicationDto dto = createBaseDto();
        dto.setCategory(ApplicationCategory.NEW);

        LicenseApplication app = LicenseApplicationFactory.create(dto, testUser, "LIC-20261007-12345");

        assertNotNull(app);
        assertEquals(ApplicationCategory.NEW, app.getCategory());
        assertEquals("LIC-20261007-12345", app.getReferenceNumber());
        assertEquals("Thujikoshan Yogathas", app.getFullName());
        assertEquals("B (Car/Van)", app.getVehicleClass());
        assertEquals(ApplicationStatus.SUBMITTED, app.getStatus());
        assertFalse(app.getIsDraft());
        assertNull(app.getExistingLicenseNumber());
        assertNull(app.getPoliceReportRef());
    }

    @Test
    void testCreateRenewalLicenseApplicationSuccess() {
        LicenseApplicationDto dto = createBaseDto();
        dto.setCategory(ApplicationCategory.RENEWAL);
        dto.setExistingLicenseNumber("B1234567");

        LicenseApplication app = LicenseApplicationFactory.create(dto, testUser, "LIC-20261007-12346");

        assertNotNull(app);
        assertEquals(ApplicationCategory.RENEWAL, app.getCategory());
        assertEquals("B1234567", app.getExistingLicenseNumber());
        assertNull(app.getPoliceReportRef());
    }

    @Test
    void testCreateRenewalLicenseApplicationMissingExistingNumber() {
        LicenseApplicationDto dto = createBaseDto();
        dto.setCategory(ApplicationCategory.RENEWAL);
        dto.setExistingLicenseNumber(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> LicenseApplicationFactory.create(dto, testUser, "LIC-20261007-12347"));
        assertTrue(ex.getMessage().contains("Existing driving license number is required"));
    }

    @Test
    void testCreateLostLicenseApplicationSuccess() {
        LicenseApplicationDto dto = createBaseDto();
        dto.setCategory(ApplicationCategory.LOST);
        dto.setExistingLicenseNumber("B1234567");
        dto.setPoliceReportRef("POL-COLOMBO-9988");

        LicenseApplication app = LicenseApplicationFactory.create(dto, testUser, "LIC-20261007-12348");

        assertNotNull(app);
        assertEquals(ApplicationCategory.LOST, app.getCategory());
        assertEquals("POL-COLOMBO-9988", app.getPoliceReportRef());
        assertEquals("B1234567", app.getExistingLicenseNumber());
    }

    @Test
    void testCreateLostLicenseApplicationMissingPoliceReport() {
        LicenseApplicationDto dto = createBaseDto();
        dto.setCategory(ApplicationCategory.LOST);
        dto.setPoliceReportRef(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> LicenseApplicationFactory.create(dto, testUser, "LIC-20261007-12349"));
        assertTrue(ex.getMessage().contains("Police report reference number is required"));
    }
}
