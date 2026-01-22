package com.example.carrental.dto.car;

import com.example.carrental.enums.CarType;
import com.example.carrental.validation.annotation.UkrainianCarPlate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CarRequestDto {
    @NotBlank
    private String brand;

    @NotBlank
    private String model;

    @NotBlank
    private CarType carType;

    @NotBlank
    private String color;

    @NotBlank
    @UkrainianCarPlate
    private String licensePlate;

    @NotNull
    @Positive
    private BigDecimal dailyFee;

}
