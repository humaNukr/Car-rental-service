package com.example.carrental.controller;

import com.example.carrental.dto.payment.CreateFineDto;
import com.example.carrental.dto.payment.PaymentRequestDto;
import com.example.carrental.dto.payment.PaymentResponseDto;
import com.example.carrental.service.impl.StripeWebhookService;
import com.example.carrental.service.interfaces.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final StripeWebhookService stripeWebhookService;

    @Operation(summary = "Create payment session", description = "Calculates amount and returns Stripe URL")
    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponseDto createPaymentSession(@RequestBody @Valid PaymentRequestDto requestDto) {
        return paymentService.createPaymentSession(requestDto);
    }

    @Operation(summary = "Handle success redirect", description = "Checks Stripe status and updates DB")
    @GetMapping("/success")
    @ResponseStatus(HttpStatus.OK)
    public PaymentResponseDto handleSuccess(@RequestParam("session_id") String sessionId) {
        return paymentService.handlePaymentSuccess(sessionId);
    }

    @Operation(summary = "Handle cancel redirect", description = "User cancelled payment")
    @GetMapping("/cancel")
    public String handleCancel(@RequestParam("session_id") String sessionId) {
        paymentService.handlePaymentCancel(sessionId);
        return "Payment cancelled successfully. You can close this tab or try again.";
    }

    @PostMapping("/rentals/{id}/fine")
    @PreAuthorize("hasRole('MANAGER')")
    public PaymentResponseDto issueFine(
            @PathVariable Long id,
            @RequestBody @Valid CreateFineDto fineDto) {
        return paymentService.createFine(id, fineDto);
    }

    @Operation(summary = "Stripe Webhook")
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            stripeWebhookService.processWebhook(payload, sigHeader);
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook error: " + e.getMessage());
        }
    }
}
