package com.example.carrental.controller;

import com.example.carrental.dto.rental.RentalRequestDto;
import com.example.carrental.dto.rental.RentalResponseDto;
import com.example.carrental.dto.rental.RentalUpdateRequestDto;
import com.example.carrental.service.RentalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/rentals")
public class RentalController {
    private final RentalService rentalService;

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public RentalResponseDto createRental(@RequestBody @Valid RentalRequestDto rentalDto) {
        return rentalService.createRental(rentalDto);
    }

    @PatchMapping("/{id}/update")
    public RentalResponseDto updateRental(@PathVariable Long id, @RequestBody @Valid RentalUpdateRequestDto rentalDto) {
        return rentalService.updateRental(id, rentalDto);
    }

    @GetMapping("/my")
    public List<RentalResponseDto> getMyRentals(Pageable pageable) {
        return rentalService.getMyRentals(pageable);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('MANAGER')")
    public RentalResponseDto returnCar(@PathVariable Long id) {
        return rentalService.returnCar(id);
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('MANAGER')")
    public List<RentalResponseDto> getAllActive(Pageable pageable) {
        return rentalService.getAllActive(pageable);
    }

}
