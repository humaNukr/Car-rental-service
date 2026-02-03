package com.example.carrental.dto.payment;

import com.example.carrental.enums.PaymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequestDto {
    @NotNull
    private Long rentalId;

    @NotNull
    private PaymentType type;
}