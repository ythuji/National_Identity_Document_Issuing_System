package org.nidis.national_identity_document_issuing_system.controller;

import java.util.List;

import org.nidis.national_identity_document_issuing_system.model.AuditLog;
import org.nidis.national_identity_document_issuing_system.model.LicenseApplication;
import org.nidis.national_identity_document_issuing_system.model.NicApplication;
import org.nidis.national_identity_document_issuing_system.model.PassportApplication;
import org.nidis.national_identity_document_issuing_system.model.PaymentTransaction;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationStatus;
import org.nidis.national_identity_document_issuing_system.model.enums.PaymentStatus;
import org.nidis.national_identity_document_issuing_system.model.enums.RoleName;
import org.nidis.national_identity_document_issuing_system.repository.LicenseApplicationRepository;
import org.nidis.national_identity_document_issuing_system.repository.NicApplicationRepository;
import org.nidis.national_identity_document_issuing_system.repository.PassportApplicationRepository;
import org.nidis.national_identity_document_issuing_system.repository.PaymentTransactionRepository;
import org.nidis.national_identity_document_issuing_system.repository.UserRepository;
import org.nidis.national_identity_document_issuing_system.service.AdminUserService;
import org.nidis.national_identity_document_issuing_system.service.AuditLogService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminUserService adminUserService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final LicenseApplicationRepository licenseRepository;
    private final PassportApplicationRepository passportRepository;
    private final NicApplicationRepository nicRepository;
    private final PaymentTransactionRepository paymentRepository;

    @GetMapping({"", "/", "/dashboard"})
    @Transactional(readOnly = true)
    public String adminDashboard(Model model) {
        List<User> allUsers = userRepository.findAll();
        long totalUsers = allUsers.size();
        long activeUsers = allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count();
        long totalOfficers = allUsers.stream().filter(u -> u.getRoles().stream().anyMatch(r -> r.getRoleName() == RoleName.VERIFICATION_OFFICER)).count();
        long totalApplicants = allUsers.stream().filter(u -> u.getRoles().stream().anyMatch(r -> r.getRoleName() == RoleName.APPLICANT)).count();

        List<LicenseApplication> licenses = licenseRepository.findAll();
        List<PassportApplication> passports = passportRepository.findAll();
        List<NicApplication> nics = nicRepository.findAll();

        long totalLicenseApps = licenses.stream().filter(a -> !Boolean.TRUE.equals(a.getIsDraft())).count();
        long totalPassportApps = passports.stream().filter(a -> !Boolean.TRUE.equals(a.getIsDraft())).count();
        long totalNicApps = nics.stream().filter(a -> !Boolean.TRUE.equals(a.getIsDraft())).count();
        long totalApplications = totalLicenseApps + totalPassportApps + totalNicApps;

        long pendingCount = licenses.stream().filter(a -> a.getStatus() == ApplicationStatus.SUBMITTED || a.getStatus() == ApplicationStatus.UNDER_REVIEW || a.getStatus() == ApplicationStatus.PENDING_PAYMENT).count()
                + passports.stream().filter(a -> a.getStatus() == ApplicationStatus.SUBMITTED || a.getStatus() == ApplicationStatus.UNDER_REVIEW || a.getStatus() == ApplicationStatus.PENDING_PAYMENT).count()
                + nics.stream().filter(a -> a.getStatus() == ApplicationStatus.SUBMITTED || a.getStatus() == ApplicationStatus.UNDER_REVIEW || a.getStatus() == ApplicationStatus.PENDING_PAYMENT).count();

        long approvedCount = licenses.stream().filter(a -> a.getStatus() == ApplicationStatus.APPROVED).count()
                + passports.stream().filter(a -> a.getStatus() == ApplicationStatus.APPROVED).count()
                + nics.stream().filter(a -> a.getStatus() == ApplicationStatus.APPROVED).count();

        long rejectedCount = licenses.stream().filter(a -> a.getStatus() == ApplicationStatus.REJECTED || a.getStatus() == ApplicationStatus.PENDING_CORRECTION).count()
                + passports.stream().filter(a -> a.getStatus() == ApplicationStatus.REJECTED || a.getStatus() == ApplicationStatus.PENDING_CORRECTION).count()
                + nics.stream().filter(a -> a.getStatus() == ApplicationStatus.REJECTED || a.getStatus() == ApplicationStatus.PENDING_CORRECTION).count();

        List<PaymentTransaction> payments = paymentRepository.findAll();
        Double totalRevenue = payments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PAID && p.getAmount() != null)
                .mapToDouble(PaymentTransaction::getAmount)
                .sum();

        List<AuditLog> recentLogs = auditLogService.getRecentLogs();
        if (recentLogs.size() > 8) {
            recentLogs = recentLogs.subList(0, 8);
        }

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("totalOfficers", totalOfficers);
        model.addAttribute("totalApplicants", totalApplicants);
        model.addAttribute("totalLicenseApps", totalLicenseApps);
        model.addAttribute("totalPassportApps", totalPassportApps);
        model.addAttribute("totalNicApps", totalNicApps);
        model.addAttribute("totalApplications", totalApplications);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalPayments", payments.size());
        model.addAttribute("recentLogs", recentLogs);

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(@RequestParam(value = "search", required = false) String search,
                            Model model) {
        List<User> users = adminUserService.searchUsers(search);
        model.addAttribute("users", users);
        model.addAttribute("searchQuery", search != null ? search : "");
        model.addAttribute("allRoles", RoleName.values());
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable("id") Long id,
                                   @AuthenticationPrincipal UserDetails adminDetails,
                                   RedirectAttributes redirectAttributes) {
        try {
            adminUserService.toggleUserStatus(id, adminDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "User account active status toggled successfully.");
        } catch (Exception ex) {
            log.error("Failed to toggle user status: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/roles")
    public String updateRoles(@PathVariable("id") Long id,
                              @RequestParam(value = "roles", required = false) List<String> roleNames,
                              @AuthenticationPrincipal UserDetails adminDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            adminUserService.updateUserRoles(id, roleNames != null ? roleNames : List.of("APPLICANT"), adminDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "User roles updated successfully.");
        } catch (Exception ex) {
            log.error("Failed to update user roles: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/audit-log")
    public String auditLog(Model model) {
        List<AuditLog> logs = auditLogService.getRecentLogs();
        model.addAttribute("logs", logs);
        return "admin/audit-log";
    }
}

