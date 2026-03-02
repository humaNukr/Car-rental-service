package com.example.carrental.dto.car;

import com.example.carrental.enums.car.FuelType;
import com.example.carrental.enums.car.TransmissionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CarSpecificationDto {
    @NotNull(message = "Transmission type is required")
    private TransmissionType transmission;

    @NotNull(message = "Fuel type is required")
    private FuelType fuelType;

    @NotNull
    @Min(1)
    private Integer seatingCapacity;

    @NotNull
    @Min(1)
    private Integer doorsQuantity;

    @NotNull
    @Min(0)
    private Integer bagQuantity;

    @NotNull
    private Boolean hasAirConditioning;
}