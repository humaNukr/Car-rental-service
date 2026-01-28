package com.example.carrental.dto.rental;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RentalUpdateRequestDto {
    @NotNull
    @Future
    private LocalDate returnDate;
}
