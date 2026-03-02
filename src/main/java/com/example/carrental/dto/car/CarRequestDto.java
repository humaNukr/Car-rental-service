package com.example.carrental.dto.car;

import com.example.carrental.enums.car.CarClass;
import com.example.carrental.enums.car.CarType;
import com.example.carrental.validation.annotation.UkrainianCarPlate;
import com.example.carrental.validation.annotation.ValidEnum;
import jakarta.validation.Valid;
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

    @ValidEnum(enumClass = CarType.class)
    private CarType type;

    @NotBlank
    private String color;

    @NotBlank
    @UkrainianCarPlate
    private String licensePlate;

    @NotNull
    @Positive
    private BigDecimal dailyFee;

    @NotNull
    @Positive
    private Long locationId;

    @NotNull(message = "Car class is required")
    private CarClass carClass;

    @NotNull(message = "Specification is required")
    @Valid
    private CarSpecificationDto specification;

}
