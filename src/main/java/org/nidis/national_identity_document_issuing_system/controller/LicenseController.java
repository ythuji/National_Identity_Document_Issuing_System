package org.nidis.national_identity_document_issuing_system.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nidis.national_identity_document_issuing_system.dto.ApplicationCorrectionDto;
import org.nidis.national_identity_document_issuing_system.dto.LicenseApplicationDto;
import org.nidis.national_identity_document_issuing_system.model.ApplicationDocument;
import org.nidis.national_identity_document_issuing_system.model.LicenseApplication;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationCategory;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;
import org.nidis.national_identity_document_issuing_system.service.FileStorageService;
import org.nidis.national_identity_document_issuing_system.service.LicenseService;
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
@RequestMapping("/license")
@RequiredArgsConstructor
@Slf4j
public class LicenseController {

    private final LicenseService licenseService;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final PaymentService paymentService;

    @GetMapping
    public String selectCategoryPage() {
        return "license/select-category";
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
            return "redirect:/license";
        }

        LicenseApplicationDto dto = licenseService.getDraftOrNewForm(user, category);
        model.addAttribute("licenseDto", dto);
        model.addAttribute("category", category);

        switch (category) {
            case RENEWAL:
                return "license/apply-renewal";
            case LOST:
                return "license/apply-lost";
            default:
                return "license/apply-new";
        }
    }

    @PostMapping("/apply")
    public String processApplication(@Valid @ModelAttribute("licenseDto") LicenseApplicationDto dto,
                                     BindingResult bindingResult,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();

        if (bindingResult.hasErrors()) {
            model.addAttribute("category", dto.getCategory());
            if (dto.getCategory() == ApplicationCategory.RENEWAL) return "license/apply-renewal";
            if (dto.getCategory() == ApplicationCategory.LOST) return "license/apply-lost";
            return "license/apply-new";
        }

        try {
            LicenseApplication application = licenseService.submitApplication(dto, user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Application submitted successfully! Reference Number: " + application.getReferenceNumber());
            return "redirect:/license/view/" + application.getId();
        } catch (Exception ex) {
            log.error("Failed to submit license application: ", ex);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("category", dto.getCategory());
            if (dto.getCategory() == ApplicationCategory.RENEWAL) return "license/apply-renewal";
            if (dto.getCategory() == ApplicationCategory.LOST) return "license/apply-lost";
            return "license/apply-new";
        }
    }

    @PostMapping("/draft")
    public String saveDraft(@ModelAttribute("licenseDto") LicenseApplicationDto dto,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        try {
            LicenseApplication savedDraft = licenseService.saveAsDraft(dto, user);
            redirectAttributes.addFlashAttribute("infoMessage",
                    "Application saved as draft (Ref: " + savedDraft.getReferenceNumber() + "). You can resume it anytime.");
            return "redirect:/license/my-applications";
        } catch (Exception ex) {
            log.error("Failed to save draft: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save draft: " + ex.getMessage());
            return "redirect:/license";
        }
    }

    @GetMapping("/view/{id}")
    public String viewApplicationDetail(@PathVariable("id") Long id,
                                        @AuthenticationPrincipal UserDetails userDetails,
                                        Model model) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        LicenseApplication app = licenseService.getApplicationById(id)
                .orElseThrow(() -> new IllegalArgumentException("License application not found."));

        // Ensure user can view their own application or officer can inspect
        boolean isOwner = app.getUser().getId().equals(user.getId());
        boolean isOfficerOrAdmin = user.getRoles().stream().anyMatch(r ->
                r.getRoleName().name().equals("VERIFICATION_OFFICER") || r.getRoleName().name().equals("SUPER_ADMIN"));

        if (!isOwner && !isOfficerOrAdmin) {
            return "redirect:/dashboard";
        }

        List<ApplicationDocument> documents = fileStorageService.getDocumentsByApplication(app.getId(), ApplicationType.LICENSE);
        model.addAttribute("app", app);
        model.addAttribute("application", app);
        model.addAttribute("documents", documents);
        model.addAttribute("paymentStatus", paymentService.getLatestPaymentForApp(app.getId(), ApplicationType.LICENSE)
            .map(transaction -> transaction.getPaymentStatus().name()).orElse("PENDING"));
        return "license/application-detail";
    }

    @PostMapping("/correct/{id}")
    public String submitCorrection(@PathVariable("id") Long id,
                                   @ModelAttribute("correctionDto") ApplicationCorrectionDto dto,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        try {
            LicenseApplication updated = licenseService.submitCorrection(id, dto, user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Corrections submitted successfully! Application " + updated.getReferenceNumber() + " is now Under Review.");
        } catch (Exception ex) {
            log.error("Failed to submit corrections for License application #{}: ", id, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Error submitting corrections: " + ex.getMessage());
        }
        return "redirect:/license/view/" + id;
    }

    @GetMapping("/my-applications")
    public String myApplicationsPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        List<LicenseApplication> applications = licenseService.getApplicationsByUser(user.getId());
        model.addAttribute("applications", applications);
        return "license/my-applications";
    }

    @PostMapping("/delete-draft/{id}")
    public String deleteDraft(@PathVariable("id") Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        try {
            licenseService.deleteDraft(id, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Draft discarded successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/license/my-applications";
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
    public ResponseEntity<?> prefillRenewal(@RequestParam("licenseNumber") String licenseNumber) {
        Optional<LicenseApplication> appOpt = licenseService.prefillForRenewal(licenseNumber);
        if (appOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        LicenseApplication app = appOpt.get();
        return ResponseEntity.ok(app);
    }
}

