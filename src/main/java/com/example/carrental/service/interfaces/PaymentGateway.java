package com.example.carrental.service.interfaces;

import com.example.carrental.dto.payment.PaymentSessionInfoDto;
import com.example.carrental.enums.payment.PaymentType;
import java.math.BigDecimal;

public interface PaymentGateway {
    PaymentSessionInfoDto createSession(BigDecimal amount, String description, PaymentType type);
    boolean isPaymentSuccessful(String sessionId);
    void expireSession(String sessionId);
}