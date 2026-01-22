package com.example.carrental.service;

import com.example.carrental.dto.car.CarRequestDto;
import com.example.carrental.dto.car.CarResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CarService {
    CarResponseDto save(CarRequestDto dto);

    List<CarResponseDto> getAll(Pageable pageable);

    CarResponseDto getById(Long id);

    CarResponseDto update(Long id, CarRequestDto dto);

    void delete(Long id);
}
