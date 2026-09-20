package org.nidis.national_identity_document_issuing_system.controller;

import java.util.List;

import org.nidis.national_identity_document_issuing_system.dto.PaymentRequestDto;
import org.nidis.national_identity_document_issuing_system.model.PaymentTransaction;
import org.nidis.national_identity_document_issuing_system.model.User;
import org.nidis.national_identity_document_issuing_system.model.enums.ApplicationType;
import org.nidis.national_identity_document_issuing_system.service.PaymentService;
import org.nidis.national_identity_document_issuing_system.service.UserService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    @GetMapping("/checkout/{type}/{id}")
    public String checkoutPage(@PathVariable("type") String typeStr,
                               @PathVariable("id") Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        ApplicationType type;
        try {
            type = ApplicationType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return "redirect:/dashboard";
        }

        try {
            PaymentRequestDto dto = paymentService.prepareCheckout(type, id, user);
            model.addAttribute("paymentDto", dto);
            return "payment/checkout";
        } catch (Exception ex) {
            log.error("Failed to prepare payment checkout: ", ex);
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/process")
    public String processPayment(@Valid @ModelAttribute("paymentDto") PaymentRequestDto dto,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();

        if (bindingResult.hasErrors()) {
            return "payment/checkout";
        }

        try {
            PaymentTransaction txn = paymentService.processPayment(dto, user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment processed successfully! Transaction ID: " + txn.getTransactionId());
            return "redirect:/payment/receipt/" + txn.getTransactionId();
        } catch (Exception ex) {
            log.error("Payment processing error: ", ex);
            String msg = ex.getMessage() != null ? ex.getMessage() : "Payment was declined by issuing bank.";
            if (msg.toLowerCase().contains("cvv")) {
                bindingResult.rejectValue("cvv", "error.paymentDto", msg);
            } else {
                bindingResult.rejectValue("cardNumber", "error.paymentDto", msg);
            }
            return "payment/checkout";
        }
    }

    @GetMapping("/receipt/{txnId}")
    public String receiptPage(@PathVariable("txnId") String txnId,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        PaymentTransaction txn = paymentService.getTransactionByTxnId(txnId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found."));

        boolean isOwner = txn.getUser().getId().equals(user.getId());
        boolean isOfficerOrAdmin = user.getRoles().stream().anyMatch(r ->
                r.getRoleName().name().equals("VERIFICATION_OFFICER") || r.getRoleName().name().equals("SUPER_ADMIN"));

        if (!isOwner && !isOfficerOrAdmin) {
            return "redirect:/dashboard";
        }

        model.addAttribute("transaction", txn);
        return "payment/receipt";
    }

    @GetMapping("/my-payments")
    public String myPaymentsPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        List<PaymentTransaction> transactions = paymentService.getUserTransactions(user.getId());
        model.addAttribute("transactions", transactions);
        return "payment/my-payments";
    }
}

