package com.example.carrental.dto.payment;

import com.example.carrental.enums.PaymentStatus;
import com.example.carrental.enums.PaymentType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentResponseDto {
    private Long id;
    private PaymentStatus status;
    private PaymentType type;
    private BigDecimal amount;
    private String sessionUrl;
    private String sessionId;
    private Long rentalId;
}