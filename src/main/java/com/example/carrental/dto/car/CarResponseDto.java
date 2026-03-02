package com.example.carrental.dto.car;

import com.example.carrental.enums.car.CarClass;
import com.example.carrental.enums.car.CarStatus;
import com.example.carrental.enums.car.CarType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CarResponseDto {
    private Long id;
    private String brand;
    private String model;
    private CarType type;
    private String color;
    private String licensePlate;
    private CarStatus status;
    private BigDecimal dailyFee;
    private String mainImageUrl;
    private Long locationId;
    private CarClass carClass;
    private CarSpecificationDto specification;
}
