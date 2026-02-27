package com.example.carrental.dto.location;

import com.example.carrental.validation.annotation.LocationConsistency;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@LocationConsistency
public class LocationUpdateRequestDto {
    @Size(min = 1)
    private String city;
    @Size(min = 1)
    private String address;
    private String workHours;
    @Email
    private String email;
    private List<String> phones;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
