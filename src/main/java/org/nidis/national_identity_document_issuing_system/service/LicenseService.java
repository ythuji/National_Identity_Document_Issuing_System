package org.nidis.national_identity_document_issuing_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nidis.national_identity_document_issuing_system.dto.ApplicationCorrectionDto;
import org.nidis.national_identity_document_issuing_system.dto.LicenseApplicationDto;
import org.nidis.national_identity_document_issuing_system.model.LicenseApplication;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationCategory;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationStatus;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;
import org.nidis.national_identity_document_issuing_system.repository.LicenseApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LicenseService {

    private final LicenseApplicationRepository licenseRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public LicenseApplication submitApplication(LicenseApplicationDto dto, User user) throws IOException {
        validateSubmissionRules(dto);

        LicenseApplication application;
        if (dto.getId() != null) {
            application = licenseRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Draft application not found."));
            if (!application.getUser().getId().equals(user.getId())) {
                throw new IllegalStateException("Unauthorized to update this application.");
            }
        } else {
            application = new LicenseApplication();
            application.setUser(user);
            application.setReferenceNumber(generateReferenceNumber());
        }

        application.setCategory(dto.getCategory());
        application.setFullName(dto.getFullName().trim());
        application.setDateOfBirth(dto.getDateOfBirth());
        application.setAddress(dto.getAddress().trim());
        application.setPhone(dto.getPhone().trim());
        application.setVehicleClass(dto.getVehicleClass().trim());
        application.setExistingLicenseNumber(dto.getExistingLicenseNumber() != null ? dto.getExistingLicenseNumber().trim() : null);
        application.setPoliceReportRef(dto.getPoliceReportRef() != null ? dto.getPoliceReportRef().trim() : null);
        application.setMedicalCertRequired(Boolean.TRUE.equals(dto.getMedicalCertRequired()));
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setIsDraft(false);

        LicenseApplication saved = licenseRepository.save(application);

        // Upload attached documents directly to MS SQL database
        saveAttachedDocuments(saved.getId(), dto);

        // Notify user
        notificationService.sendNotification(user.getId(),
                "Driver's License Application Submitted (" + saved.getReferenceNumber() + ")",
                "Your " + saved.getCategory() + " driver's license application with reference number "
                        + saved.getReferenceNumber() + " has been successfully submitted and is queued for verification.");

        log.info("License application [{}] successfully submitted by user: {}", saved.getReferenceNumber(), user.getEmail());
        return saved;
    }

    @Transactional
    public LicenseApplication saveAsDraft(LicenseApplicationDto dto, User user) throws IOException {
        LicenseApplication application;
        if (dto.getId() != null) {
            application = licenseRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Application not found"));
            if (!application.getUser().getId().equals(user.getId())) {
                throw new IllegalStateException("Unauthorized");
            }
        } else {
            application = new LicenseApplication();
            application.setUser(user);
            application.setReferenceNumber(generateReferenceNumber());
        }

        application.setCategory(dto.getCategory() != null ? dto.getCategory() : ApplicationCategory.NEW);
        application.setFullName(dto.getFullName() != null ? dto.getFullName().trim() : user.getFullName());
        application.setDateOfBirth(dto.getDateOfBirth() != null ? dto.getDateOfBirth() : LocalDate.of(2000, 1, 1));
        application.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : "Pending");
        application.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : (user.getPhone() != null ? user.getPhone() : "Pending"));
        application.setVehicleClass(dto.getVehicleClass() != null ? dto.getVehicleClass().trim() : "B");
        application.setExistingLicenseNumber(dto.getExistingLicenseNumber());
        application.setPoliceReportRef(dto.getPoliceReportRef());
        application.setMedicalCertRequired(Boolean.TRUE.equals(dto.getMedicalCertRequired()));
        application.setStatus(ApplicationStatus.DRAFT);
        application.setIsDraft(true);

        LicenseApplication saved = licenseRepository.save(application);
        saveAttachedDocuments(saved.getId(), dto);

        log.info("License application [{}] saved as draft for user: {}", saved.getReferenceNumber(), user.getEmail());
        return saved;
    }

    public LicenseApplicationDto getDraftOrNewForm(User user, ApplicationCategory category) {
        Optional<LicenseApplication> draftOpt = licenseRepository.findFirstByUserIdAndCategoryAndIsDraftTrue(user.getId(), category);
        if (draftOpt.isPresent()) {
            LicenseApplication draft = draftOpt.get();
            LicenseApplicationDto dto = new LicenseApplicationDto();
            dto.setId(draft.getId());
            dto.setCategory(draft.getCategory());
            dto.setFullName(draft.getFullName());
            dto.setDateOfBirth(draft.getDateOfBirth());
            dto.setAddress(draft.getAddress());
            dto.setPhone(draft.getPhone());
            dto.setVehicleClass(draft.getVehicleClass());
            dto.setExistingLicenseNumber(draft.getExistingLicenseNumber());
            dto.setPoliceReportRef(draft.getPoliceReportRef());
            dto.setMedicalCertRequired(draft.getMedicalCertRequired());
            return dto;
        }

        LicenseApplicationDto newDto = new LicenseApplicationDto();
        newDto.setCategory(category);
        newDto.setFullName(user.getFullName());
        newDto.setPhone(user.getPhone());
        return newDto;
    }

    public Optional<LicenseApplication> prefillForRenewal(String existingLicenseNumber) {
        if (existingLicenseNumber == null || existingLicenseNumber.isBlank()) return Optional.empty();
        String trimmed = existingLicenseNumber.trim();
        Optional<LicenseApplication> byIssued = licenseRepository.findTopByIssuedLicenseNumberOrderByCreatedAtDesc(trimmed);
        if (byIssued.isPresent()) return byIssued;
        Optional<LicenseApplication> byLic = licenseRepository.findTopByExistingLicenseNumberOrderByCreatedAtDesc(trimmed);
        if (byLic.isPresent()) return byLic;
        return licenseRepository.findByReferenceNumber(trimmed);
    }

    public List<LicenseApplication> getApplicationsByUser(Long userId) {
        return licenseRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<LicenseApplication> getApplicationById(Long id) {
        return licenseRepository.findById(id);
    }

    @Transactional
    public void deleteDraft(Long id, Long userId) {
        LicenseApplication app = licenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        if (!app.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Unauthorized");
        }

        if (!Boolean.TRUE.equals(app.getIsDraft())) {
            throw new IllegalStateException("Only draft applications can be discarded.");
        }

        fileStorageService.deleteDocumentsByApplication(app.getId(), ApplicationType.LICENSE);
        licenseRepository.delete(app);
        log.info("Draft license application #{} deleted", id);
    }

    @Transactional
    public LicenseApplication submitCorrection(Long id, ApplicationCorrectionDto dto, User user) throws IOException {
        LicenseApplication application = licenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("License application not found."));

        if (!application.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Unauthorized: You do not own this application.");
        }

        if (application.getStatus() != ApplicationStatus.PENDING_CORRECTION && application.getStatus() != ApplicationStatus.DRAFT) {
            throw new IllegalStateException("Application is not in a state requiring correction (Current status: " + application.getStatus() + ").");
        }

        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            application.setPhone(dto.getPhone().trim());
        }
        if (dto.getAddress() != null && !dto.getAddress().isBlank()) {
            application.setAddress(dto.getAddress().trim());
        }

        // Upload any new replacement files
        if (dto.getPhotoFile() != null && !dto.getPhotoFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getPhotoFile(), application.getId(), ApplicationType.LICENSE, "PHOTO");
        }
        if (dto.getAddressProofFile() != null && !dto.getAddressProofFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getAddressProofFile(), application.getId(), ApplicationType.LICENSE, "ID_PROOF");
        }
        if (dto.getMedicalCertFile() != null && !dto.getMedicalCertFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getMedicalCertFile(), application.getId(), ApplicationType.LICENSE, "MEDICAL_CERT");
        }
        if (dto.getPoliceReportFile() != null && !dto.getPoliceReportFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getPoliceReportFile(), application.getId(), ApplicationType.LICENSE, "POLICE_REPORT");
        }

        // Note from applicant
        if (dto.getApplicantNote() != null && !dto.getApplicantNote().isBlank()) {
            String prevComment = application.getOfficerComment() != null ? application.getOfficerComment() : "";
            application.setOfficerComment(prevComment + " [Applicant Note: " + dto.getApplicantNote().trim() + "]");
        }

        // Once corrected, set status back to UNDER_REVIEW
        application.setStatus(ApplicationStatus.UNDER_REVIEW);
        LicenseApplication saved = licenseRepository.save(application);

        notificationService.sendNotification(user.getId(),
                "Driver's License Application Resubmitted (" + saved.getReferenceNumber() + ")",
                "Your corrections for driver's license application (" + saved.getReferenceNumber()
                        + ") have been received. The application is now Under Review by our verification team.");

        log.info("License application [{}] resubmitted with corrections by user {}", saved.getReferenceNumber(), user.getEmail());
        return saved;
    }

    private void saveAttachedDocuments(Long applicationId, LicenseApplicationDto dto) throws IOException {
        if (dto.getPhotoFile() != null && !dto.getPhotoFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getPhotoFile(), applicationId, ApplicationType.LICENSE, "PHOTO");
        }
        if (dto.getIdProofFile() != null && !dto.getIdProofFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getIdProofFile(), applicationId, ApplicationType.LICENSE, "ID_PROOF");
        }
        if (dto.getMedicalCertFile() != null && !dto.getMedicalCertFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getMedicalCertFile(), applicationId, ApplicationType.LICENSE, "MEDICAL_CERT");
        }
        if (dto.getPoliceReportFile() != null && !dto.getPoliceReportFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getPoliceReportFile(), applicationId, ApplicationType.LICENSE, "POLICE_REPORT");
        }
    }

    private void validateSubmissionRules(LicenseApplicationDto dto) {
        if (dto.getCategory() == ApplicationCategory.RENEWAL) {
            if (dto.getExistingLicenseNumber() == null || dto.getExistingLicenseNumber().isBlank()) {
                throw new IllegalArgumentException("Existing driving license number is required for license renewal.");
            }
        } else if (dto.getCategory() == ApplicationCategory.LOST) {
            if (dto.getPoliceReportRef() == null || dto.getPoliceReportRef().isBlank()) {
                throw new IllegalArgumentException("Police report reference number is required for lost license replacements.");
            }
        }
    }

    public String generateReferenceNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = secureRandom.nextInt(90000) + 10000;
        return "LIC-" + dateStr + "-" + rand;
    }
}

