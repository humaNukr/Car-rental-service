package com.example.carrental.dto.car;

import com.example.carrental.enums.CarStatus;
import com.example.carrental.enums.CarType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CarDetailsDto {
    private Long id;
    private String brand;
    private String model;
    private CarType type;
    private String color;
    private String licensePlate;
    private CarStatus status;
    private BigDecimal dailyFee;
    private List<String> imageUrls;
}
