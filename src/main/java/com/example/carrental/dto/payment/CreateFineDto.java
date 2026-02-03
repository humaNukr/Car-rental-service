package com.example.carrental.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateFineDto(
        @NotNull
        @DecimalMin("0.01")
        BigDecimal amount,

        String type
) {
}