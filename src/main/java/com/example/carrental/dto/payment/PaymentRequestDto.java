package com.example.carrental.dto.payment;

import com.example.carrental.enums.PaymentType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequestDto {
    @NotNull
    private Long rentalId;

    @NotNull
    private PaymentType type;
}