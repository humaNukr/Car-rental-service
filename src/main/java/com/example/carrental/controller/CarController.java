package com.example.carrental.controller;

import com.example.carrental.dto.car.CarRequestDto;
import com.example.carrental.dto.car.CarResponseDto;
import com.example.carrental.dto.car.CarSearchParameters;
import com.example.carrental.service.CarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
public class CarController {
    private final CarService carService;

    @GetMapping
    public List<CarResponseDto> getAll(
            @ModelAttribute CarSearchParameters searchParameters,
            Pageable pageable
    ) {
        return carService.getAll(searchParameters, pageable);
    }

    @GetMapping("/{id}")
    public CarResponseDto getById(@PathVariable Long id) {
        return carService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    public CarResponseDto createCar(@RequestBody @Valid CarRequestDto carRequestDto) {
        return carService.save(carRequestDto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public CarResponseDto updateCar(@PathVariable Long id, @RequestBody @Valid CarRequestDto carRequestDto) {
        return carService.update(id, carRequestDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MANAGER')")
    public void delete(@PathVariable Long id) {
        carService.delete(id);
    }
}
