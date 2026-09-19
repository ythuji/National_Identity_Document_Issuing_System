package org.nidis.national_identity_document_issuing_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nidis.national_identity_document_issuing_system.model.ApplicationDocument;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;
import org.nidis.national_identity_document_issuing_system.repository.ApplicationDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final ApplicationDocumentRepository documentRepository;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB limit

    @Transactional
    public ApplicationDocument saveDocument(MultipartFile file, Long applicationId, ApplicationType type, String documentType) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        validateFile(file);

        // Delete existing document of this type if updating draft
        documentRepository.findByApplicationIdAndApplicationTypeAndDocumentType(applicationId, type, documentType)
                .ifPresent(documentRepository::delete);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = documentType + ".dat";
        }

        ApplicationDocument doc = ApplicationDocument.builder()
                .applicationId(applicationId)
                .applicationType(type)
                .documentName(originalFilename)
                .documentType(documentType)
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .fileData(file.getBytes())
                .fileSize(file.getSize())
                .build();

        ApplicationDocument saved = documentRepository.save(doc);
        log.info("Saved document [{} - {}] directly into MS SQL database for application #{}", documentType, originalFilename, applicationId);
        return saved;
    }

    public Optional<ApplicationDocument> getDocument(Long documentId) {
        return documentRepository.findById(documentId);
    }

    public List<ApplicationDocument> getDocumentsByApplication(Long applicationId, ApplicationType type) {
        return documentRepository.findByApplicationIdAndApplicationType(applicationId, type);
    }

    @Transactional
    public void deleteDocumentsByApplication(Long applicationId, ApplicationType type) {
        documentRepository.deleteByApplicationIdAndApplicationType(applicationId, type);
    }

    public void validateFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 10 MB limit.");
        }

        String contentType = file.getContentType();
        if (contentType != null) {
            boolean isAllowed = contentType.startsWith("image/") ||
                    contentType.equals("application/pdf") ||
                    contentType.equals("application/msword") ||
                    contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            if (!isAllowed) {
                throw new IllegalArgumentException("Unsupported file type: " + contentType + ". Allowed: JPG, PNG, PDF.");
            }
        }
    }
}

