package com.example.carrental.controller;

import com.example.carrental.dto.payment.PaymentRequestDto;
import com.example.carrental.dto.payment.PaymentResponseDto;
import com.example.carrental.dto.rental.RentalRequestDto;
import com.example.carrental.dto.rental.RentalResponseDto;
import com.example.carrental.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

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
}
