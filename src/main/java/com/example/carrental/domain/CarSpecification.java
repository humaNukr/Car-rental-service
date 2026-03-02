package com.example.carrental.domain;

import com.example.carrental.enums.car.FuelType;
import com.example.carrental.enums.car.TransmissionType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

@Embeddable
@Data
public class CarSpecification {

    @Enumerated(EnumType.STRING)
    @Column(name = "transmission_type", nullable = false)
    private TransmissionType transmission;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false)
    private FuelType fuelType;

    @Column(name = "seating_capacity", nullable = false)
    private Integer seatingCapacity;

    @Column(name = "doors_quantity", nullable = false)
    private Integer doorsQuantity;

    @Column(name = "bag_quantity", nullable = false)
    private Integer bagQuantity;

    @Column(name = "has_air_conditioning", nullable = false)
    private boolean hasAirConditioning;
}