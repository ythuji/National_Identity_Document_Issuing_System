package org.nidis.national_identity_document_issuing_system.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nidis.national_identity_document_issuing_system.dto.ApplicationCorrectionDto;
import org.nidis.national_identity_document_issuing_system.dto.PassportApplicationDto;
import org.nidis.national_identity_document_issuing_system.model.ApplicationDocument;
import org.nidis.national_identity_document_issuing_system.model.PassportApplication;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationCategory;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;
import org.nidis.national_identity_document_issuing_system.service.FileStorageService;
import org.nidis.national_identity_document_issuing_system.service.PassportService;
import org.nidis.national_identity_document_issuing_system.service.PaymentService;
import org.nidis.national_identity_document_issuing_system.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/passport")
@RequiredArgsConstructor
@Slf4j
public class PassportController {

    private final PassportService passportService;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final PaymentService paymentService;

    @GetMapping
    public String selectCategoryPage() {
        return "passport/select-category";
    }

    @GetMapping("/apply/{category}")
    public String applyFormPage(@PathVariable("category") String categoryStr,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        ApplicationCategory category;
        try {
            category = ApplicationCategory.valueOf(categoryStr.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return "redirect:/passport";
        }

        PassportApplicationDto dto = passportService.getDraftOrNewForm(user, category);
        model.addAttribute("passportDto", dto);
        model.addAttribute("category", category);

        switch (category) {
            case RENEWAL:
                return "passport/apply-renewal";
            case LOST:
                return "passport/apply-lost";
            default:
                return "passport/apply-new";
        }
    }

    @PostMapping("/apply")
    public String processApplication(@Valid @ModelAttribute("passportDto") PassportApplicationDto dto,
                                     BindingResult bindingResult,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();

        if (bindingResult.hasErrors()) {
            model.addAttribute("category", dto.getCategory());
            if (dto.getCategory() == ApplicationCategory.RENEWAL) return "passport/apply-renewal";
            if (dto.getCategory() == ApplicationCategory.LOST) return "passport/apply-lost";
            return "passport/apply-new";
        }

        try {
            PassportApplication application = passportService.submitApplication(dto, user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Passport application submitted successfully! Reference: " + application.getReferenceNumber());
            return "redirect:/passport/view/" + application.getId();
        } catch (Exception ex) {
            log.error("Failed to submit passport application: ", ex);
            String msg = ex.getMessage() != null ? ex.getMessage() : "Failed to submit passport application.";
            if ((dto.getCategory() == ApplicationCategory.RENEWAL || dto.getCategory() == ApplicationCategory.LOST) && msg.toLowerCase().contains("passport")) {
                bindingResult.rejectValue("existingPassportNumber", "error.passportDto", msg);
            } else if (dto.getCategory() == ApplicationCategory.LOST && msg.toLowerCase().contains("police")) {
                bindingResult.rejectValue("policeReportRef", "error.passportDto", msg);
            } else {
                bindingResult.rejectValue("fullName", "error.passportDto", msg);
            }
            model.addAttribute("category", dto.getCategory());
            if (dto.getCategory() == ApplicationCategory.RENEWAL) return "passport/apply-renewal";
            if (dto.getCategory() == ApplicationCategory.LOST) return "passport/apply-lost";
            return "passport/apply-new";
        }
    }

    @PostMapping("/draft")
    public String saveDraft(@ModelAttribute("passportDto") PassportApplicationDto dto,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        try {
            PassportApplication savedDraft = passportService.saveAsDraft(dto, user);
            redirectAttributes.addFlashAttribute("infoMessage",
                    "Passport application draft saved (Ref: " + savedDraft.getReferenceNumber() + ").");
            return "redirect:/passport/my-applications";
        } catch (Exception ex) {
            log.error("Failed to save passport draft: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save draft: " + ex.getMessage());
            return "redirect:/passport";
        }
    }

    @GetMapping("/view/{id}")
    public String viewApplicationDetail(@PathVariable("id") Long id,
                                        @AuthenticationPrincipal UserDetails userDetails,
                                        Model model) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        PassportApplication app = passportService.getApplicationById(id)
                .orElseThrow(() -> new IllegalArgumentException("Passport application not found."));

        boolean isOwner = app.getUser().getId().equals(user.getId());
        boolean isOfficerOrAdmin = user.getRoles().stream().anyMatch(r ->
                r.getRoleName().name().equals("VERIFICATION_OFFICER") || r.getRoleName().name().equals("SUPER_ADMIN"));

        if (!isOwner && !isOfficerOrAdmin) {
            return "redirect:/dashboard";
        }

        List<ApplicationDocument> documents = fileStorageService.getDocumentsByApplication(app.getId(), ApplicationType.PASSPORT);
        model.addAttribute("app", app);
        model.addAttribute("application", app);
        model.addAttribute("documents", documents);
        model.addAttribute("paymentStatus", paymentService.getLatestPaymentForApp(app.getId(), ApplicationType.PASSPORT)
            .map(transaction -> transaction.getPaymentStatus().name()).orElse("PENDING"));
        return "passport/application-detail";
    }

    @PostMapping("/correct/{id}")
    public String submitCorrection(@PathVariable("id") Long id,
                                   @ModelAttribute("correctionDto") ApplicationCorrectionDto dto,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        try {
            PassportApplication updated = passportService.submitCorrection(id, dto, user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Corrections submitted successfully! Application " + updated.getReferenceNumber() + " is now Under Review.");
        } catch (Exception ex) {
            log.error("Failed to submit corrections for Passport application #{}: ", id, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Error submitting corrections: " + ex.getMessage());
        }
        return "redirect:/passport/view/" + id;
    }

    @GetMapping("/my-applications")
    public String myApplicationsPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        List<PassportApplication> applications = passportService.getApplicationsByUser(user.getId());
        model.addAttribute("applications", applications);
        return "passport/my-applications";
    }

    @PostMapping("/delete-draft/{id}")
    public String deleteDraft(@PathVariable("id") Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        try {
            passportService.deleteDraft(id, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Draft discarded successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/passport/my-applications";
    }

    @GetMapping("/document/{docId}")
    public ResponseEntity<byte[]> viewDocument(@PathVariable("docId") Long docId) {
        Optional<ApplicationDocument> docOpt = fileStorageService.getDocument(docId);
        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ApplicationDocument doc = docOpt.get();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getDocumentName() + "\"")
                .contentType(MediaType.parseMediaType(doc.getContentType()))
                .body(doc.getFileData());
    }

    @GetMapping("/prefill")
    @ResponseBody
    public ResponseEntity<?> prefillRenewal(@RequestParam("passportNumber") String passportNumber) {
        Optional<PassportApplication> appOpt = passportService.prefillBioData(passportNumber);
        if (appOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(appOpt.get());
    }
}

