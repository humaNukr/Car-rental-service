package com.example.carrental.dto.car;

import com.example.carrental.enums.car.CarClass;
import com.example.carrental.enums.car.CarStatus;
import com.example.carrental.enums.car.CarType;
import com.example.carrental.enums.car.FuelType;
import com.example.carrental.enums.car.TransmissionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CarSearchParameters(
        String[] brands,
        String[] models,
        CarType[] types,
        String[] colors,
        CarStatus status,
        BigDecimal minDailyFee,
        BigDecimal maxDailyFee,
        LocalDate startDate,
        LocalDate endDate,
        CarClass[] carClasses,
        TransmissionType[] transmissions,
        FuelType[] fuelTypes,
        Boolean hasAirConditioning
) {
}
