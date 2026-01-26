package com.example.carrental.dto.rental;

import com.example.carrental.validation.annotation.ValidRentalPeriod;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@ValidRentalPeriod
public class RentalUpdateRequestDto {
    @NotNull
    private LocalDate rentalDate;

    @NotNull
    @Future
    private LocalDate returnDate;
}
