package com.example.carrental.service;

import com.example.carrental.dto.rental.RentalRequestDto;
import com.example.carrental.dto.rental.RentalResponseDto;
import com.example.carrental.dto.rental.RentalUpdateRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RentalService {
    RentalResponseDto createRental(RentalRequestDto requestDto);

    RentalResponseDto updateRental(Long id, RentalUpdateRequestDto requestDto);

    List<RentalResponseDto> getMyRentals(Pageable pageable);

    RentalResponseDto returnCar(Long rentalId);

    List<RentalResponseDto> getAllActive(Pageable pageable);
}
