package com.example.carrental.dto.car;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class CarUpdateRequestDto {
    private String color;

    @Positive
    private BigDecimal dailyFee;
}
