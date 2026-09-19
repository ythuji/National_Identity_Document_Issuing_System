package org.nidis.national_identity_document_issuing_system.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.nidis.national_identity_document_issuing_system.dto.DashboardApplicationItem;
import org.nidis.national_identity_document_issuing_system.model.LicenseApplication;
import org.nidis.national_identity_document_issuing_system.model.NicApplication;
import org.nidis.national_identity_document_issuing_system.model.PassportApplication;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationStatus;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;
import org.nidis.national_identity_document_issuing_system.repository.LicenseApplicationRepository;
import org.nidis.national_identity_document_issuing_system.repository.NicApplicationRepository;
import org.nidis.national_identity_document_issuing_system.repository.PassportApplicationRepository;
import org.nidis.national_identity_document_issuing_system.service.NotificationService;
import org.nidis.national_identity_document_issuing_system.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;
    private final NotificationService notificationService;
    private final LicenseApplicationRepository licenseRepository;
    private final PassportApplicationRepository passportRepository;
    private final NicApplicationRepository nicRepository;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> "SUPER_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            return "redirect:/admin/dashboard";
        }

        boolean isOfficer = userDetails.getAuthorities().stream()
                .anyMatch(a -> "VERIFICATION_OFFICER".equals(a.getAuthority()));
        if (isOfficer) {
            return "redirect:/verification/queue";
        }

        User user = userService.findByEmail(userDetails.getUsername()).orElse(null);
        model.addAttribute("user", user);

        if (user != null) {
            long unreadNotifications = notificationService.getUnreadCount(user.getId());
            model.addAttribute("unreadNotifications", unreadNotifications);

            // Fetch user applications across all 3 services
            List<LicenseApplication> licenseApps = licenseRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            List<PassportApplication> passportApps = passportRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            List<NicApplication> nicApps = nicRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

            List<DashboardApplicationItem> items = new ArrayList<>();

            for (LicenseApplication app : licenseApps) {
                if (!Boolean.TRUE.equals(app.getIsDraft())) {
                    items.add(DashboardApplicationItem.builder()
                            .id(app.getId())
                            .referenceNumber(app.getReferenceNumber())
                            .serviceName("Driver's License")
                            .serviceType(ApplicationType.LICENSE)
                            .category(app.getCategory().name())
                            .createdAt(app.getCreatedAt())
                            .status(app.getStatus())
                            .viewUrl("/license/view/" + app.getId())
                            .build());
                }
            }

            for (PassportApplication app : passportApps) {
                if (!Boolean.TRUE.equals(app.getIsDraft())) {
                    items.add(DashboardApplicationItem.builder()
                            .id(app.getId())
                            .referenceNumber(app.getReferenceNumber())
                            .serviceName("Passport Services")
                            .serviceType(ApplicationType.PASSPORT)
                            .category(app.getCategory().name())
                            .createdAt(app.getCreatedAt())
                            .status(app.getStatus())
                            .viewUrl("/passport/view/" + app.getId())
                            .build());
                }
            }

            for (NicApplication app : nicApps) {
                if (!Boolean.TRUE.equals(app.getIsDraft())) {
                    items.add(DashboardApplicationItem.builder()
                            .id(app.getId())
                            .referenceNumber(app.getReferenceNumber())
                            .serviceName("NIC Services")
                            .serviceType(ApplicationType.NIC)
                            .category(app.getCategory().name())
                            .createdAt(app.getCreatedAt())
                            .status(app.getStatus())
                            .viewUrl("/nic/view/" + app.getId())
                            .build());
                }
            }

            // Sort newest first
            items.sort(Comparator.comparing(DashboardApplicationItem::getCreatedAt).reversed());

            long total = items.size();
            long pending = items.stream().filter(i -> i.getStatus() == ApplicationStatus.SUBMITTED
                    || i.getStatus() == ApplicationStatus.UNDER_REVIEW
                    || i.getStatus() == ApplicationStatus.PENDING_PAYMENT).count();
            long approved = items.stream().filter(i -> i.getStatus() == ApplicationStatus.APPROVED).count();
            long rejected = items.stream().filter(i -> i.getStatus() == ApplicationStatus.REJECTED
                    || i.getStatus() == ApplicationStatus.PENDING_CORRECTION).count();

            model.addAttribute("totalApplications", total);
            model.addAttribute("pendingApplications", pending);
            model.addAttribute("approvedApplications", approved);
            model.addAttribute("rejectedApplications", rejected);
            model.addAttribute("recentApplications", items);
        } else {
            model.addAttribute("totalApplications", 0);
            model.addAttribute("pendingApplications", 0);
            model.addAttribute("approvedApplications", 0);
            model.addAttribute("rejectedApplications", 0);
            model.addAttribute("recentApplications", List.of());
        }

        return "dashboard/index";
    }
}
