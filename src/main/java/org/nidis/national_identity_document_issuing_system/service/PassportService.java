package org.nidis.national_identity_document_issuing_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nidis.national_identity_document_issuing_system.dto.ApplicationCorrectionDto;
import org.nidis.national_identity_document_issuing_system.dto.PassportApplicationDto;
import org.nidis.national_identity_document_issuing_system.model.PassportApplication;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationCategory;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationStatus;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;
import org.nidis.national_identity_document_issuing_system.repository.PassportApplicationRepository;
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
public class PassportService {

    private final PassportApplicationRepository passportRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public PassportApplication submitApplication(PassportApplicationDto dto, User user) throws IOException {
        validateSubmissionRules(dto);

        PassportApplication application;
        if (dto.getId() != null) {
            application = passportRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Draft application not found."));
            if (!application.getUser().getId().equals(user.getId())) {
                throw new IllegalStateException("Unauthorized");
            }
        } else {
            application = new PassportApplication();
            application.setUser(user);
            application.setReferenceNumber(generateReferenceNumber());
        }

        application.setCategory(dto.getCategory());
        application.setFullName(dto.getFullName().trim());
        application.setDateOfBirth(dto.getDateOfBirth());
        application.setPlaceOfBirth(dto.getPlaceOfBirth().trim());
        application.setNationality(dto.getNationality() != null ? dto.getNationality().trim() : "Sri Lankan");
        application.setGender(dto.getGender());
        application.setAddress(dto.getAddress().trim());
        application.setPhone(dto.getPhone().trim());
        application.setProfession(dto.getProfession() != null ? dto.getProfession().trim() : null);
        application.setDualCitizenship(Boolean.TRUE.equals(dto.getDualCitizenship()));
        application.setExistingPassportNumber(dto.getExistingPassportNumber() != null ? dto.getExistingPassportNumber().trim() : null);
        application.setPoliceReportRef(dto.getPoliceReportRef() != null ? dto.getPoliceReportRef().trim() : null);
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setIsDraft(false);

        PassportApplication saved = passportRepository.save(application);

        // Upload attached documents directly to MS SQL database
        saveAttachedDocuments(saved.getId(), dto);

        // Notify user
        notificationService.sendNotification(user.getId(),
                "Passport Application Submitted (" + saved.getReferenceNumber() + ")",
                "Your " + saved.getCategory() + " passport application with reference number "
                        + saved.getReferenceNumber() + " has been successfully submitted and routed to verification.");

        log.info("Passport application [{}] successfully submitted by user: {}", saved.getReferenceNumber(), user.getEmail());
        return saved;
    }

    @Transactional
    public PassportApplication saveAsDraft(PassportApplicationDto dto, User user) throws IOException {
        PassportApplication application;
        if (dto.getId() != null) {
            application = passportRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Application not found"));
            if (!application.getUser().getId().equals(user.getId())) {
                throw new IllegalStateException("Unauthorized");
            }
        } else {
            application = new PassportApplication();
            application.setUser(user);
            application.setReferenceNumber(generateReferenceNumber());
        }

        application.setCategory(dto.getCategory() != null ? dto.getCategory() : ApplicationCategory.NEW);
        application.setFullName(dto.getFullName() != null ? dto.getFullName().trim() : user.getFullName());
        application.setDateOfBirth(dto.getDateOfBirth() != null ? dto.getDateOfBirth() : LocalDate.of(1995, 1, 1));
        application.setPlaceOfBirth(dto.getPlaceOfBirth() != null ? dto.getPlaceOfBirth().trim() : "Colombo");
        application.setNationality(dto.getNationality() != null ? dto.getNationality().trim() : "Sri Lankan");
        application.setGender(dto.getGender() != null ? dto.getGender() : "Male");
        application.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : "Pending");
        application.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : (user.getPhone() != null ? user.getPhone() : "Pending"));
        application.setProfession(dto.getProfession());
        application.setDualCitizenship(Boolean.TRUE.equals(dto.getDualCitizenship()));
        application.setExistingPassportNumber(dto.getExistingPassportNumber());
        application.setPoliceReportRef(dto.getPoliceReportRef());
        application.setStatus(ApplicationStatus.DRAFT);
        application.setIsDraft(true);

        PassportApplication saved = passportRepository.save(application);
        saveAttachedDocuments(saved.getId(), dto);

        log.info("Passport application [{}] saved as draft for user: {}", saved.getReferenceNumber(), user.getEmail());
        return saved;
    }

    public PassportApplicationDto getDraftOrNewForm(User user, ApplicationCategory category) {
        Optional<PassportApplication> draftOpt = passportRepository.findFirstByUserIdAndCategoryAndIsDraftTrue(user.getId(), category);
        if (draftOpt.isPresent()) {
            PassportApplication draft = draftOpt.get();
            PassportApplicationDto dto = new PassportApplicationDto();
            dto.setId(draft.getId());
            dto.setCategory(draft.getCategory());
            dto.setFullName(draft.getFullName());
            dto.setDateOfBirth(draft.getDateOfBirth());
            dto.setPlaceOfBirth(draft.getPlaceOfBirth());
            dto.setNationality(draft.getNationality());
            dto.setGender(draft.getGender());
            dto.setAddress(draft.getAddress());
            dto.setPhone(draft.getPhone());
            dto.setProfession(draft.getProfession());
            dto.setDualCitizenship(draft.getDualCitizenship());
            dto.setExistingPassportNumber(draft.getExistingPassportNumber());
            dto.setPoliceReportRef(draft.getPoliceReportRef());
            return dto;
        }

        PassportApplicationDto newDto = new PassportApplicationDto();
        newDto.setCategory(category);
        newDto.setFullName(user.getFullName());
        newDto.setPhone(user.getPhone());
        newDto.setNationality("Sri Lankan");
        return newDto;
    }

    public Optional<PassportApplication> prefillBioData(String existingPassportNumber) {
        if (existingPassportNumber == null || existingPassportNumber.isBlank()) return Optional.empty();
        String trimmed = existingPassportNumber.trim();
        Optional<PassportApplication> byIssued = passportRepository.findTopByIssuedPassportNumberOrderByCreatedAtDesc(trimmed);
        if (byIssued.isPresent()) return byIssued;
        Optional<PassportApplication> byPass = passportRepository.findTopByExistingPassportNumberOrderByCreatedAtDesc(trimmed);
        if (byPass.isPresent()) return byPass;
        return passportRepository.findByReferenceNumber(trimmed);
    }

    public List<PassportApplication> getApplicationsByUser(Long userId) {
        return passportRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<PassportApplication> getApplicationById(Long id) {
        return passportRepository.findById(id);
    }

    @Transactional
    public void deleteDraft(Long id, Long userId) {
        PassportApplication app = passportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        if (!app.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Unauthorized");
        }

        if (!Boolean.TRUE.equals(app.getIsDraft())) {
            throw new IllegalStateException("Only draft applications can be discarded.");
        }

        fileStorageService.deleteDocumentsByApplication(app.getId(), ApplicationType.PASSPORT);
        passportRepository.delete(app);
        log.info("Draft passport application #{} discarded", id);
    }

    @Transactional
    public PassportApplication submitCorrection(Long id, ApplicationCorrectionDto dto, User user) throws IOException {
        PassportApplication application = passportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Passport application not found."));

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
        if (dto.getOccupation() != null && !dto.getOccupation().isBlank()) {
            application.setProfession(dto.getOccupation().trim());
        }

        // Upload any new replacement files
        if (dto.getPhotoFile() != null && !dto.getPhotoFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getPhotoFile(), application.getId(), ApplicationType.PASSPORT, "PHOTO");
        }
        if (dto.getBirthCertFile() != null && !dto.getBirthCertFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getBirthCertFile(), application.getId(), ApplicationType.PASSPORT, "BIRTH_CERT");
        }
        if (dto.getAddressProofFile() != null && !dto.getAddressProofFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getAddressProofFile(), application.getId(), ApplicationType.PASSPORT, "ID_PROOF");
        }
        if (dto.getPoliceReportFile() != null && !dto.getPoliceReportFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getPoliceReportFile(), application.getId(), ApplicationType.PASSPORT, "POLICE_REPORT");
        }

        // Note from applicant
        if (dto.getApplicantNote() != null && !dto.getApplicantNote().isBlank()) {
            String prevComment = application.getOfficerComment() != null ? application.getOfficerComment() : "";
            application.setOfficerComment(prevComment + " [Applicant Note: " + dto.getApplicantNote().trim() + "]");
        }

        // Once corrected, set status back to UNDER_REVIEW
        application.setStatus(ApplicationStatus.UNDER_REVIEW);
        PassportApplication saved = passportRepository.save(application);

        notificationService.sendNotification(user.getId(),
                "Passport Application Resubmitted (" + saved.getReferenceNumber() + ")",
                "Your corrections for Passport application (" + saved.getReferenceNumber()
                        + ") have been received. The application is now Under Review by our verification team.");

        log.info("Passport application [{}] resubmitted with corrections by user {}", saved.getReferenceNumber(), user.getEmail());
        return saved;
    }

    private void saveAttachedDocuments(Long applicationId, PassportApplicationDto dto) throws IOException {
        if (dto.getPhotoFile() != null && !dto.getPhotoFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getPhotoFile(), applicationId, ApplicationType.PASSPORT, "PHOTO");
        }
        if (dto.getNicCopyFile() != null && !dto.getNicCopyFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getNicCopyFile(), applicationId, ApplicationType.PASSPORT, "ID_PROOF");
        }
        if (dto.getBirthCertFile() != null && !dto.getBirthCertFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getBirthCertFile(), applicationId, ApplicationType.PASSPORT, "BIRTH_CERT");
        }
        if (dto.getPoliceReportFile() != null && !dto.getPoliceReportFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getPoliceReportFile(), applicationId, ApplicationType.PASSPORT, "POLICE_REPORT");
        }
    }

    private void validateSubmissionRules(PassportApplicationDto dto) {
        if (dto.getCategory() == ApplicationCategory.RENEWAL) {
            if (dto.getExistingPassportNumber() == null || dto.getExistingPassportNumber().isBlank()) {
                throw new IllegalArgumentException("Existing passport number is required for passport renewal.");
            }
        } else if (dto.getCategory() == ApplicationCategory.LOST) {
            if (dto.getPoliceReportRef() == null || dto.getPoliceReportRef().isBlank()) {
                throw new IllegalArgumentException("Police incident reference is mandatory for lost passport replacements.");
            }
        }
    }

    public String generateReferenceNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = secureRandom.nextInt(90000) + 10000;
        return "PPT-" + dateStr + "-" + rand;
    }
}

