package org.nidis.national_identity_document_issuing_system.controller;

import java.util.List;
import java.util.Optional;

import org.nidis.national_identity_document_issuing_system.dto.ApplicationCorrectionDto;
import org.nidis.national_identity_document_issuing_system.dto.NicApplicationDto;
import org.nidis.national_identity_document_issuing_system.model.ApplicationDocument;
import org.nidis.national_identity_document_issuing_system.model.NicApplication;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationCategory;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;
import org.nidis.national_identity_document_issuing_system.service.FileStorageService;
import org.nidis.national_identity_document_issuing_system.service.NicService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/nic")
@RequiredArgsConstructor
@Slf4j
public class NicController {

    private final NicService nicService;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final PaymentService paymentService;

    @GetMapping
    public String selectCategoryPage() {
        return "nic/select-category";
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
            return "redirect:/nic";
        }

        NicApplicationDto dto = nicService.getDraftOrNewForm(user, category);
        model.addAttribute("nicDto", dto);
        model.addAttribute("category", category);

        switch (category) {
            case RENEWAL:
                return "nic/apply-renewal";
            case LOST:
                return "nic/apply-lost";
            default:
                return "nic/apply-new";
        }
    }

    @PostMapping("/apply")
    public String processApplication(@Valid @ModelAttribute("nicDto") NicApplicationDto dto,
                                     BindingResult bindingResult,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();

        if (bindingResult.hasErrors()) {
            model.addAttribute("category", dto.getCategory());
            if (dto.getCategory() == ApplicationCategory.RENEWAL) return "nic/apply-renewal";
            if (dto.getCategory() == ApplicationCategory.LOST) return "nic/apply-lost";
            return "nic/apply-new";
        }

        try {
            NicApplication application = nicService.submitApplication(dto, user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "NIC application submitted! Reference: " + application.getReferenceNumber() + ". Please proceed with payment.");
            return "redirect:/nic/view/" + application.getId();
        } catch (Exception ex) {
            log.error("Failed to submit NIC application: ", ex);
            String msg = ex.getMessage() != null ? ex.getMessage() : "Failed to submit NIC application.";
            if ((dto.getCategory() == ApplicationCategory.RENEWAL || dto.getCategory() == ApplicationCategory.LOST) && msg.toLowerCase().contains("nic")) {
                bindingResult.rejectValue("existingNicNumber", "error.nicDto", msg);
            } else if (dto.getCategory() == ApplicationCategory.LOST && msg.toLowerCase().contains("police")) {
                bindingResult.rejectValue("policeReportRef", "error.nicDto", msg);
            } else {
                bindingResult.rejectValue("fullName", "error.nicDto", msg);
            }
            model.addAttribute("category", dto.getCategory());
            if (dto.getCategory() == ApplicationCategory.RENEWAL) return "nic/apply-renewal";
            if (dto.getCategory() == ApplicationCategory.LOST) return "nic/apply-lost";
            return "nic/apply-new";
        }
    }

    @PostMapping("/draft")
    public String saveDraft(@ModelAttribute("nicDto") NicApplicationDto dto,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        try {
            NicApplication savedDraft = nicService.saveAsDraft(dto, user);
            redirectAttributes.addFlashAttribute("infoMessage",
                    "NIC application draft saved (Ref: " + savedDraft.getReferenceNumber() + ").");
            return "redirect:/nic/my-applications";
        } catch (Exception ex) {
            log.error("Failed to save NIC draft: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save draft: " + ex.getMessage());
            return "redirect:/nic";
        }
    }

    @GetMapping("/view/{id}")
    public String viewApplicationDetail(@PathVariable("id") Long id,
                                        @AuthenticationPrincipal UserDetails userDetails,
                                        Model model) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        NicApplication app = nicService.getApplicationById(id)
                .orElseThrow(() -> new IllegalArgumentException("NIC application not found."));

        boolean isOwner = app.getUser().getId().equals(user.getId());
        boolean isOfficerOrAdmin = user.getRoles().stream().anyMatch(r ->
                r.getRoleName().name().equals("VERIFICATION_OFFICER") || r.getRoleName().name().equals("SUPER_ADMIN"));

        if (!isOwner && !isOfficerOrAdmin) {
            return "redirect:/dashboard";
        }

        List<ApplicationDocument> documents = fileStorageService.getDocumentsByApplication(app.getId(), ApplicationType.NIC);
        model.addAttribute("app", app);
        model.addAttribute("application", app);
        model.addAttribute("documents", documents);
        model.addAttribute("paymentStatus", paymentService.getLatestPaymentForApp(app.getId(), ApplicationType.NIC)
            .map(transaction -> transaction.getPaymentStatus().name()).orElse(app.getPaymentStatus().name()));
        return "nic/application-detail";
    }

    @PostMapping("/correct/{id}")
    public String submitCorrection(@PathVariable("id") Long id,
                                   @ModelAttribute("correctionDto") ApplicationCorrectionDto dto,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        try {
            NicApplication updated = nicService.submitCorrection(id, dto, user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Corrections submitted successfully! Application " + updated.getReferenceNumber() + " is now Under Review.");
        } catch (Exception ex) {
            log.error("Failed to submit corrections for NIC application #{}: ", id, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Error submitting corrections: " + ex.getMessage());
        }
        return "redirect:/nic/view/" + id;
    }

    @GetMapping("/my-applications")
    public String myApplicationsPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        List<NicApplication> applications = nicService.getApplicationsByUser(user.getId());
        model.addAttribute("applications", applications);
        return "nic/my-applications";
    }

    @PostMapping("/delete-draft/{id}")
    public String deleteDraft(@PathVariable("id") Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        try {
            nicService.deleteDraft(id, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Draft discarded successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/nic/my-applications";
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
    public ResponseEntity<?> prefillRenewal(@RequestParam("nicNumber") String nicNumber) {
        Optional<NicApplication> appOpt = nicService.prefillBioData(nicNumber);
        if (appOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(appOpt.get());
    }
}

