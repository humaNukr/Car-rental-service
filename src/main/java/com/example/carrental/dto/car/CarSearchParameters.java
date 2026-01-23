package com.example.carrental.dto.car;

import com.example.carrental.enums.CarStatus;
import com.example.carrental.enums.CarType;

import java.math.BigDecimal;

public record CarSearchParameters(
        String[] brands,
        String[] models,
        CarType[] types,
        String[] colors,
        CarStatus status,
        BigDecimal minDailyFee,
        BigDecimal maxDailyFee
) {
}
