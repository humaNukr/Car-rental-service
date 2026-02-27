package com.example.carrental.dto.location;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class LocationRequestDto {
    @NotBlank
    private String city;
    @NotBlank
    private String address;
    @NotBlank
    private String workHours;
    @NotBlank
    @Email
    private String email;
    @NotNull
    private List<String> phones;
    @NotNull
    private BigDecimal latitude;
    @NotNull
    private BigDecimal longitude;
}
