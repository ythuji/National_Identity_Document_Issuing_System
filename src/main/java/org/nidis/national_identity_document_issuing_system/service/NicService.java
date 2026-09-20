package org.nidis.national_identity_document_issuing_system.service;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.nidis.national_identity_document_issuing_system.dto.ApplicationCorrectionDto;
import org.nidis.national_identity_document_issuing_system.dto.NicApplicationDto;
import org.nidis.national_identity_document_issuing_system.model.NicApplication;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationCategory;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationStatus;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;
import org.nidis.national_identity_document_issuing_system.model.enums.PaymentStatus;
import org.nidis.national_identity_document_issuing_system.repository.NicApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NicService {

    private final NicApplicationRepository nicRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public NicApplication submitApplication(NicApplicationDto dto, User user) throws IOException {
        validateSubmissionRules(dto);

        NicApplication application;
        if (dto.getId() != null) {
            application = nicRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Draft application not found."));
            if (!application.getUser().getId().equals(user.getId())) {
                throw new IllegalStateException("Unauthorized");
            }
        } else {
            application = new NicApplication();
            application.setUser(user);
            application.setReferenceNumber(generateReferenceNumber());
        }

        application.setCategory(dto.getCategory());
        application.setFullName(dto.getFullName().trim());
        application.setDateOfBirth(dto.getDateOfBirth());
        application.setGender(dto.getGender());
        application.setCivilStatus(dto.getCivilStatus());
        application.setOccupation(dto.getOccupation() != null ? dto.getOccupation().trim() : null);
        application.setPermanentAddress(dto.getPermanentAddress().trim());
        application.setPhone(dto.getPhone().trim());
        application.setGramaNiladhariDivision(dto.getGramaNiladhariDivision().trim());
        application.setDivisionalSecretariat(dto.getDivisionalSecretariat().trim());
        application.setExistingNicNumber(dto.getExistingNicNumber() != null ? dto.getExistingNicNumber().trim() : null);
        application.setPoliceReportRef(dto.getPoliceReportRef() != null ? dto.getPoliceReportRef().trim() : null);
        application.setStatus(ApplicationStatus.PENDING_PAYMENT);
        application.setPaymentStatus(PaymentStatus.PENDING);
        application.setFeeAmount(calculateFee(dto.getCategory()));
        application.setIsDraft(false);

        NicApplication saved = nicRepository.save(application);

        // Upload attached documents directly to MS SQL database
        saveAttachedDocuments(saved.getId(), dto);

        // Notify user
        notificationService.sendNotification(user.getId(),
                "NIC Application Submitted (" + saved.getReferenceNumber() + ")",
                "Your " + saved.getCategory() + " NIC application (" + saved.getReferenceNumber()
                        + ") has been submitted. Please complete payment to proceed with verification.");

        log.info("NIC application [{}] submitted by user: {}", saved.getReferenceNumber(), user.getEmail());
        return saved;
    }

    @Transactional
    public NicApplication saveAsDraft(NicApplicationDto dto, User user) throws IOException {
        NicApplication application;
        if (dto.getId() != null) {
            application = nicRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Application not found"));
            if (!application.getUser().getId().equals(user.getId())) {
                throw new IllegalStateException("Unauthorized");
            }
        } else {
            application = new NicApplication();
            application.setUser(user);
            application.setReferenceNumber(generateReferenceNumber());
        }

        application.setCategory(dto.getCategory() != null ? dto.getCategory() : ApplicationCategory.NEW);
        application.setFullName(dto.getFullName() != null ? dto.getFullName().trim() : user.getFullName());
        application.setDateOfBirth(dto.getDateOfBirth() != null ? dto.getDateOfBirth() : LocalDate.of(2000, 1, 1));
        application.setGender(dto.getGender() != null ? dto.getGender() : "Male");
        application.setCivilStatus(dto.getCivilStatus() != null ? dto.getCivilStatus() : "Single");
        application.setOccupation(dto.getOccupation());
        application.setPermanentAddress(dto.getPermanentAddress() != null ? dto.getPermanentAddress().trim() : "Pending");
        application.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : (user.getPhone() != null ? user.getPhone() : "Pending"));
        application.setGramaNiladhariDivision(dto.getGramaNiladhariDivision() != null ? dto.getGramaNiladhariDivision().trim() : "Pending");
        application.setDivisionalSecretariat(dto.getDivisionalSecretariat() != null ? dto.getDivisionalSecretariat().trim() : "Pending");
        application.setExistingNicNumber(dto.getExistingNicNumber());
        application.setPoliceReportRef(dto.getPoliceReportRef());
        application.setStatus(ApplicationStatus.DRAFT);
        application.setFeeAmount(calculateFee(application.getCategory()));
        application.setIsDraft(true);

        NicApplication saved = nicRepository.save(application);
        saveAttachedDocuments(saved.getId(), dto);

        log.info("NIC application [{}] saved as draft for user: {}", saved.getReferenceNumber(), user.getEmail());
        return saved;
    }

    public NicApplicationDto getDraftOrNewForm(User user, ApplicationCategory category) {
        Optional<NicApplication> draftOpt = nicRepository.findByUserIdAndCategoryAndIsDraftTrue(user.getId(), category);
        if (draftOpt.isPresent()) {
            NicApplication draft = draftOpt.get();
            NicApplicationDto dto = new NicApplicationDto();
            dto.setId(draft.getId());
            dto.setCategory(draft.getCategory());
            dto.setFullName(draft.getFullName());
            dto.setDateOfBirth(draft.getDateOfBirth());
            dto.setGender(draft.getGender());
            dto.setCivilStatus(draft.getCivilStatus());
            dto.setOccupation(draft.getOccupation());
            dto.setPermanentAddress(draft.getPermanentAddress());
            dto.setPhone(draft.getPhone());
            dto.setGramaNiladhariDivision(draft.getGramaNiladhariDivision());
            dto.setDivisionalSecretariat(draft.getDivisionalSecretariat());
            dto.setExistingNicNumber(draft.getExistingNicNumber());
            dto.setPoliceReportRef(draft.getPoliceReportRef());
            dto.setFeeAmount(draft.getFeeAmount());
            return dto;
        }

        NicApplicationDto newDto = new NicApplicationDto();
        newDto.setCategory(category);
        newDto.setFullName(user.getFullName());
        newDto.setPhone(user.getPhone());
        newDto.setExistingNicNumber(user.getNicNumber());
        newDto.setFeeAmount(calculateFee(category));
        return newDto;
    }

    public Optional<NicApplication> prefillBioData(String existingNicNumber) {
        if (existingNicNumber == null || existingNicNumber.isBlank()) return Optional.empty();
        String trimmed = existingNicNumber.trim();
        Optional<NicApplication> byIssued = nicRepository.findTopByIssuedNicNumberOrderByCreatedAtDesc(trimmed);
        if (byIssued.isPresent()) return byIssued;
        Optional<NicApplication> byNic = nicRepository.findFirstByExistingNicNumberOrderByCreatedAtDesc(trimmed);
        if (byNic.isPresent()) return byNic;
        return nicRepository.findByReferenceNumber(trimmed);
    }

    public List<NicApplication> getApplicationsByUser(Long userId) {
        return nicRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<NicApplication> getApplicationById(Long id) {
        return nicRepository.findById(id);
    }

    @Transactional
    public void deleteDraft(Long id, Long userId) {
        NicApplication app = nicRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        if (!app.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Unauthorized");
        }

        if (!Boolean.TRUE.equals(app.getIsDraft())) {
            throw new IllegalStateException("Only draft applications can be discarded.");
        }

        fileStorageService.deleteDocumentsByApplication(app.getId(), ApplicationType.NIC);
        nicRepository.delete(app);
        log.info("Draft NIC application #{} discarded", id);
    }

    @Transactional
    public NicApplication submitCorrection(Long id, ApplicationCorrectionDto dto, User user) throws IOException {
        NicApplication application = nicRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("NIC application not found."));

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
            application.setPermanentAddress(dto.getAddress().trim());
        }
        if (dto.getOccupation() != null && !dto.getOccupation().isBlank()) {
            application.setOccupation(dto.getOccupation().trim());
        }
        if (dto.getCivilStatus() != null && !dto.getCivilStatus().isBlank()) {
            application.setCivilStatus(dto.getCivilStatus().trim());
        }

        // Upload any new replacement files
        if (dto.getPhotoFile() != null && !dto.getPhotoFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getPhotoFile(), application.getId(), ApplicationType.NIC, "PHOTO");
        }
        if (dto.getBirthCertFile() != null && !dto.getBirthCertFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getBirthCertFile(), application.getId(), ApplicationType.NIC, "BIRTH_CERT");
        }
        if (dto.getAddressProofFile() != null && !dto.getAddressProofFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getAddressProofFile(), application.getId(), ApplicationType.NIC, "ID_PROOF");
        }
        if (dto.getPoliceReportFile() != null && !dto.getPoliceReportFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getPoliceReportFile(), application.getId(), ApplicationType.NIC, "POLICE_REPORT");
        }

        // Note from applicant
        if (dto.getApplicantNote() != null && !dto.getApplicantNote().isBlank()) {
            String prevComment = application.getOfficerComment() != null ? application.getOfficerComment() : "";
            application.setOfficerComment(prevComment + " [Applicant Resubmission Note: " + dto.getApplicantNote().trim() + "]");
        }

        // Once corrected, set status back to UNDER_REVIEW
        application.setStatus(ApplicationStatus.UNDER_REVIEW);
        NicApplication saved = nicRepository.save(application);

        notificationService.sendNotification(user.getId(),
                "NIC Application Resubmitted (" + saved.getReferenceNumber() + ")",
                "Your corrections for NIC application (" + saved.getReferenceNumber()
                        + ") have been received. The application is now Under Review by our verification team.");

        log.info("NIC application [{}] resubmitted with corrections by user {}", saved.getReferenceNumber(), user.getEmail());
        return saved;
    }

    private void saveAttachedDocuments(Long applicationId, NicApplicationDto dto) throws IOException {
        if (dto.getPhotoFile() != null && !dto.getPhotoFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getPhotoFile(), applicationId, ApplicationType.NIC, "PHOTO");
        }
        if (dto.getBirthCertFile() != null && !dto.getBirthCertFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getBirthCertFile(), applicationId, ApplicationType.NIC, "BIRTH_CERT");
        }
        if (dto.getAddressProofFile() != null && !dto.getAddressProofFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getAddressProofFile(), applicationId, ApplicationType.NIC, "ID_PROOF");
        }
        if (dto.getPoliceReportFile() != null && !dto.getPoliceReportFile().isEmpty()) {
            fileStorageService.saveDocument(dto.getPoliceReportFile(), applicationId, ApplicationType.NIC, "POLICE_REPORT");
        }
    }

    private void validateSubmissionRules(NicApplicationDto dto) {
        if (dto.getCategory() == ApplicationCategory.RENEWAL) {
            if (dto.getExistingNicNumber() == null || dto.getExistingNicNumber().isBlank()) {
                throw new IllegalArgumentException("Existing NIC number is required for NIC renewal.");
            }
        } else if (dto.getCategory() == ApplicationCategory.LOST) {
            if (dto.getPoliceReportRef() == null || dto.getPoliceReportRef().isBlank()) {
                throw new IllegalArgumentException("Police incident reference is mandatory for lost NIC replacements.");
            }
        }
    }

    public Double calculateFee(ApplicationCategory category) {
        if (category == null || category == ApplicationCategory.NEW) {
            return 500.0;
        }
        return 1000.0;
    }

    public String generateReferenceNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = secureRandom.nextInt(90000) + 10000;
        return "NIC-" + dateStr + "-" + rand;
    }
}

