package com.example.carrental.dto.rental;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RentalResponseDto {
    private Long id;
    private LocalDate rentalDate;
    private LocalDate returnDate;
    private LocalDate actualReturnDate;
    private Long carId;
    private String carBrand;
    private String carModel;
    private Long userId;
}
