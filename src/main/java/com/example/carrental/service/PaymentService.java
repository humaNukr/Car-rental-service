package com.example.carrental.service;

import com.example.carrental.dto.payment.PaymentRequestDto;
import com.example.carrental.dto.payment.PaymentResponseDto;

public interface PaymentService {
    PaymentResponseDto createPaymentSession(PaymentRequestDto requestDto);

    PaymentResponseDto handlePaymentSuccess(String sessionId);
}