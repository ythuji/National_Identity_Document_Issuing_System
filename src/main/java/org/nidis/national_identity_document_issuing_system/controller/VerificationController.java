package org.nidis.national_identity_document_issuing_system.controller;

import java.util.List;

import org.nidis.national_identity_document_issuing_system.dto.VerificationDecisionDto;
import org.nidis.national_identity_document_issuing_system.dto.VerificationItemDto;
import org.nidis.national_identity_document_issuing_system.model.ApplicationDocument;
import org.nidis.national_identity_document_issuing_system.model.LicenseApplication;
import org.nidis.national_identity_document_issuing_system.model.NicApplication;
import org.nidis.national_identity_document_issuing_system.model.PassportApplication;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;
import org.nidis.national_identity_document_issuing_system.repository.LicenseApplicationRepository;
import org.nidis.national_identity_document_issuing_system.repository.NicApplicationRepository;
import org.nidis.national_identity_document_issuing_system.repository.PassportApplicationRepository;
import org.nidis.national_identity_document_issuing_system.repository.UserRepository;
import org.nidis.national_identity_document_issuing_system.service.UserService;
import org.nidis.national_identity_document_issuing_system.service.VerificationService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/verification")
@RequiredArgsConstructor
@Slf4j
public class VerificationController {

    private final VerificationService verificationService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final LicenseApplicationRepository licenseRepository;
    private final PassportApplicationRepository passportRepository;
    private final NicApplicationRepository nicRepository;

    @GetMapping("/queue")
    public String verificationQueue(@RequestParam(value = "service", required = false, defaultValue = "ALL") String service,
                                    @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
                                    @RequestParam(value = "search", required = false) String search,
                                    Model model) {
        List<VerificationItemDto> items = verificationService.getQueue(service, status, search);
        model.addAttribute("items", items);
        model.addAttribute("currentService", service);
        model.addAttribute("currentStatus", status);
        model.addAttribute("searchQuery", search != null ? search : "");

        // Counts for tabs
        model.addAttribute("countAll", verificationService.getQueue("ALL", "ALL", null).size());
        model.addAttribute("countLicense", verificationService.getQueue("LICENSE", "ALL", null).size());
        model.addAttribute("countPassport", verificationService.getQueue("PASSPORT", "ALL", null).size());
        model.addAttribute("countNic", verificationService.getQueue("NIC", "ALL", null).size());

        return "verification/queue";
    }

    @GetMapping("/review/{type}/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String reviewPage(@PathVariable("type") String typeStr,
                             @PathVariable("id") Long id,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        ApplicationType type;
        try {
            type = ApplicationType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid document service type: " + typeStr);
            return "redirect:/verification/queue";
        }

        List<ApplicationDocument> documents = verificationService.getAttachedDocuments(type, id);
        model.addAttribute("documents", documents);
        model.addAttribute("applicationType", type);
        model.addAttribute("paymentStatus", verificationService.getPaymentStatus(type, id));

        switch (type) {
            case LICENSE -> {
                LicenseApplication app = licenseRepository.findById(id).orElse(null);
                if (app == null) {
                    redirectAttributes.addFlashAttribute("errorMessage", "License application record not found (ID: " + id + ").");
                    return "redirect:/verification/queue";
                }
                User user = app.getUser() != null ? userRepository.findById(app.getUser().getId()).orElse(app.getUser()) : null;
                model.addAttribute("app", app);
                model.addAttribute("application", app);
                model.addAttribute("user", user);
                return "verification/review";
            }
            case PASSPORT -> {
                PassportApplication app = passportRepository.findById(id).orElse(null);
                if (app == null) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Passport application record not found (ID: " + id + ").");
                    return "redirect:/verification/queue";
                }
                User user = app.getUser() != null ? userRepository.findById(app.getUser().getId()).orElse(app.getUser()) : null;
                model.addAttribute("app", app);
                model.addAttribute("application", app);
                model.addAttribute("user", user);
                return "verification/review";
            }
            case NIC -> {
                NicApplication app = nicRepository.findById(id).orElse(null);
                if (app == null) {
                    redirectAttributes.addFlashAttribute("errorMessage", "NIC application record not found (ID: " + id + ").");
                    return "redirect:/verification/queue";
                }
                User user = app.getUser() != null ? userRepository.findById(app.getUser().getId()).orElse(app.getUser()) : null;
                model.addAttribute("app", app);
                model.addAttribute("application", app);
                model.addAttribute("user", user);
                return "verification/review";
            }
        }
        return "redirect:/verification/queue";
    }

    @PostMapping("/received/{type}/{id}")
    public String markReceived(@PathVariable("type") String typeStr,
                               @PathVariable("id") Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            ApplicationType type = ApplicationType.valueOf(typeStr.toUpperCase());
            User applicant = userService.findByEmail(userDetails.getUsername()).orElseThrow();
            verificationService.markReceived(type, id, applicant);
            redirectAttributes.addFlashAttribute("successMessage", "Application marked as received successfully.");
            return "redirect:/" + type.name().toLowerCase() + "/view/" + id;
        } catch (Exception ex) {
            log.error("Failed to mark application as received: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/action")
    public String processDecision(@Valid @ModelAttribute("decisionDto") VerificationDecisionDto dto,
                                  BindingResult bindingResult,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        User officer = userService.findByEmail(userDetails.getUsername()).orElseThrow();

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid action request.");
            if (dto != null && dto.getApplicationType() != null && dto.getApplicationId() != null) {
                return "redirect:/verification/review/" + dto.getApplicationType() + "/" + dto.getApplicationId();
            }
            return "redirect:/verification/queue";
        }

        try {
            verificationService.processDecision(dto, officer);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Application decision recorded successfully (" + dto.getAction() + "). Citizen notified via email & dashboard.");
            return "redirect:/verification/queue";
        } catch (Exception ex) {
            log.error("Failed to process officer decision: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + ex.getMessage());
            if (dto != null && dto.getApplicationType() != null && dto.getApplicationId() != null) {
                return "redirect:/verification/review/" + dto.getApplicationType() + "/" + dto.getApplicationId();
            }
            return "redirect:/verification/queue";
        }
    }
}

