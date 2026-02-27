package com.example.carrental.dto.location;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class LocationResponseDto {
    private Long id;
    private String city;
    private String address;
    private String workHours;
    private String email;
    private List<String> phones;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
