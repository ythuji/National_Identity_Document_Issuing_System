package org.nidis.national_identity_document_issuing_system.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otpCode, String fullName) {
        log.info("=================================================================");
        log.info("📧 [NIDIS EMAIL OTP DISPATCH]");
        log.info("Recipient: {} ({})", toEmail, fullName);
        log.info("Subject: NIDIS Registration OTP Verification Code");
        log.info("Your 6-digit OTP is: >>> {} <<< (Valid for 15 minutes)", otpCode);
        log.info("=================================================================");

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("NIDIS - Account Verification OTP Code");
            message.setText("Dear " + (fullName != null ? fullName : "Citizen") + ",\n\n"
                    + "Your One-Time Password (OTP) for NIDIS registration verification is:\n\n"
                    + "    " + otpCode + "\n\n"
                    + "This code is valid for 15 minutes. Please do not share this code with anyone.\n\n"
                    + "National Identity Document Issuing System (NIDIS)");
            mailSender.send(message);
            log.info("OTP email successfully sent via SMTP to {}", toEmail);
        } catch (Exception ex) {
            log.warn("SMTP email sending not completed ({}); OTP is accessible in application logs above.", ex.getMessage());
        }
    }

    public void sendPasswordResetEmail(String toEmail, String otpCode) {
        log.info("=================================================================");
        log.info("📧 [NIDIS PASSWORD RESET OTP]");
        log.info("Recipient: {}", toEmail);
        log.info("Password Reset OTP: >>> {} <<<", otpCode);
        log.info("=================================================================");

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("NIDIS - Password Reset Verification Code");
            message.setText("Hello,\n\n"
                    + "You requested to reset your password on NIDIS.\n"
                    + "Your verification code is: " + otpCode + "\n\n"
                    + "This code is valid for 15 minutes.\n\n"
                    + "National Identity Document Issuing System (NIDIS)");
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("SMTP email sending not completed ({}); Reset OTP is accessible in application logs above.", ex.getMessage());
        }
    }

    public void sendLoginOtpEmail(String toEmail, String otpCode, String fullName) {
        log.info("=================================================================");
        log.info("🔐 [NIDIS 2FA SIGN-IN OTP DISPATCH]");
        log.info("Recipient: {} ({})", toEmail, fullName);
        log.info("Subject: NIDIS Sign-In Verification Code (2FA)");
        log.info("Your 6-digit Sign-In OTP is: >>> {} <<< (Valid for 10 minutes)", otpCode);
        log.info("=================================================================");

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("NIDIS - Sign In Verification Code (2FA OTP)");
            message.setText("Dear " + (fullName != null ? fullName : "User") + ",\n\n"
                    + "A sign-in attempt was initiated for your NIDIS account.\n\n"
                    + "Your Two-Factor Authentication (2FA) verification code is:\n\n"
                    + "    " + otpCode + "\n\n"
                    + "This code is valid for 10 minutes. If you did not attempt to sign in, please secure your account immediately.\n\n"
                    + "National Identity Document Issuing System (NIDIS)");
            mailSender.send(message);
            log.info("2FA Login OTP email successfully sent via SMTP to {}", toEmail);
        } catch (Exception ex) {
            log.warn("SMTP email sending not completed ({}); 2FA OTP is accessible in application logs above.", ex.getMessage());
        }
    }

    public void sendDocumentIssuedEmail(String toEmail, String fullName, String documentType, String documentNumber, String referenceNumber) {
        log.info("=================================================================");
        log.info("🪪 [NIDIS OFFICIAL DOCUMENT ISSUANCE & SHIPMENT NOTICE]");
        log.info("Recipient: {} ({})", toEmail, fullName);
        log.info("Document Type: {}", documentType);
        log.info("Issued Document Number: >>> {} <<<", documentNumber);
        log.info("Application Reference: {}", referenceNumber);
        log.info("Delivery Status: SHIPPED via Postal / Courier Dispatch");
        log.info("=================================================================");

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("NIDIS - Official " + documentType + " Issued & Dispatched (" + documentNumber + ")");
            message.setText("Dear " + (fullName != null ? fullName : "Citizen") + ",\n\n"
                    + "We are pleased to inform you that your payment for application " + referenceNumber + " has been verified by the issuing officer.\n\n"
                    + "Your official Sri Lankan " + documentType + " has been issued, printed, and dispatched for delivery.\n\n"
                    + "=================================================================\n"
                    + "OFFICIAL ISSUED DOCUMENT DETAILS:\n"
                    + "  - Document Type:            " + documentType + "\n"
                    + "  - Official Document Number:  " + documentNumber + "\n"
                    + "  - Application Reference:    " + referenceNumber + "\n"
                    + "  - Status:                   SHIPPED (In Transit)\n"
                    + "=================================================================\n\n"
                    + "IMPORTANT NOTICE FOR FUTURE REFERENCE & RENEWALS:\n"
                    + "Please securely record and save your official document number: " + documentNumber + ".\n"
                    + "If you ever need to RENEW your document or apply for a duplicate replacement if it is LOST in the future, "
                    + "you can enter this same number (" + documentNumber + ") on the NIDIS portal to immediately look up and auto-fill your bio-data details.\n\n"
                    + "Once you receive your physical document at your registered address, please log into your NIDIS citizen account "
                    + "and confirm receipt by clicking 'Mark as Received'.\n\n"
                    + "Thank you for using the National Identity Document Issuing System.\n\n"
                    + "National Identity Document Issuing System (NIDIS)\n"
                    + "Government of Sri Lanka");
            mailSender.send(message);
            log.info("Official document issuance notice email successfully sent via SMTP to {}", toEmail);
        } catch (Exception ex) {
            log.warn("SMTP email sending not completed ({}); Document details logged above.", ex.getMessage());
        }
    }
}

