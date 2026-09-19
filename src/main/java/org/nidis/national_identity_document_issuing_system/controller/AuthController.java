package org.nidis.national_identity_document_issuing_system.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nidis.national_identity_document_issuing_system.dto.RegistrationDto;
import org.nidis.national_identity_document_issuing_system.dto.ResetPasswordDto;
import org.nidis.national_identity_document_issuing_system.model.OtpToken;
import org.nidis.national_identity_document_issuing_system.model.enums.OtpType;
import org.nidis.national_identity_document_issuing_system.service.OtpService;
import org.nidis.national_identity_document_issuing_system.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final OtpService otpService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "registered", required = false) String registered,
                            @RequestParam(value = "resetSuccess", required = false) String resetSuccess,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Your account has been verified and registered successfully! Please log in.");
        }
        if (resetSuccess != null) {
            model.addAttribute("successMessage", "Password reset successfully! You can now log in with your new password.");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registrationDto")) {
            model.addAttribute("registrationDto", new RegistrationDto());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("registrationDto") RegistrationDto dto,
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes) {
        if (dto.getPassword() != null && !dto.getPassword().equals(dto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.registrationDto", "Passwords do not match");
        }

        if (dto.getEmail() != null && userService.existsByEmail(dto.getEmail())) {
            bindingResult.rejectValue("email", "error.registrationDto", "An account with this email already exists");
        }

        if (dto.getNicNumber() != null && userService.existsByNicNumber(dto.getNicNumber())) {
            bindingResult.rejectValue("nicNumber", "error.registrationDto", "An account with this NIC number already exists");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            otpService.generateAndSendRegistrationOtp(dto);
            redirectAttributes.addFlashAttribute("email", dto.getEmail());
            redirectAttributes.addFlashAttribute("infoMessage", "A 6-digit verification code has been dispatched to " + dto.getEmail());
            return "redirect:/verify-otp?email=" + dto.getEmail();
        } catch (Exception ex) {
            log.error("Error during registration OTP generation: ", ex);
            bindingResult.reject("registrationError", "Failed to initiate verification code. Please try again.");
            return "auth/register";
        }
    }

    @GetMapping("/verify-otp")
    public String verifyOtpPage(@RequestParam(value = "email", required = false) String email, Model model) {
        if (email == null || email.isBlank()) {
            return "redirect:/register";
        }
        model.addAttribute("email", email);
        return "auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String processVerifyOtp(@RequestParam("email") String email,
                                   @RequestParam("otpCode") String otpCode,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            OtpToken token = otpService.verifyOtp(email, otpCode, OtpType.EMAIL_VERIFY);
            userService.createUserFromVerifiedOtp(token);
            return "redirect:/login?registered=true";
        } catch (Exception ex) {
            model.addAttribute("email", email);
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/verify-otp";
        }
    }

    @PostMapping("/resend-otp")
    public String resendOtp(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute("infoMessage", "If the email is valid, a new verification code has been dispatched.");
        } catch (Exception ignored) {
        }
        return "redirect:/verify-otp?email=" + email;
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email,
                                        RedirectAttributes redirectAttributes) {
        if (!userService.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("errorMessage", "No registered account found with that email address.");
            return "redirect:/forgot-password";
        }

        try {
            otpService.generateAndSendPasswordResetOtp(email);
            redirectAttributes.addFlashAttribute("infoMessage", "Password reset verification code sent to your email.");
            return "redirect:/reset-password?email=" + email;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to send verification code. Please try again later.");
            return "redirect:/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam("email") String email, Model model) {
        ResetPasswordDto dto = new ResetPasswordDto();
        dto.setEmail(email);
        model.addAttribute("resetPasswordDto", dto);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@Valid @ModelAttribute("resetPasswordDto") ResetPasswordDto dto,
                                       BindingResult bindingResult,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        if (dto.getNewPassword() != null && !dto.getNewPassword().equals(dto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.resetPasswordDto", "Passwords do not match");
        }

        if (bindingResult.hasErrors()) {
            return "auth/reset-password";
        }

        try {
            otpService.verifyOtp(dto.getEmail(), dto.getOtpCode(), OtpType.PASSWORD_RESET);
            userService.updatePassword(dto.getEmail(), dto.getNewPassword());
            return "redirect:/login?resetSuccess=true";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/reset-password";
        }
    }
}
