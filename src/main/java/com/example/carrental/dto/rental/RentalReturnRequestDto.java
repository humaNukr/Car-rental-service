package com.example.carrental.dto.rental;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RentalReturnRequestDto {
    Long actualLocationId;
}
