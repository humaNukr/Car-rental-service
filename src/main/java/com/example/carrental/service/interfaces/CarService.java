package com.example.carrental.service.interfaces;

import com.example.carrental.dto.car.CarRequestDto;
import com.example.carrental.dto.car.CarResponseDto;
import com.example.carrental.dto.car.CarSearchParameters;
import com.example.carrental.dto.car.CarUpdateRequestDto;
import com.example.carrental.entity.Car;
import com.example.carrental.enums.CarStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CarService {
    CarResponseDto save(CarRequestDto dto);

    List<CarResponseDto> getAll(CarSearchParameters parameters, Pageable pageable);

    CarResponseDto getById(Long id);

    CarResponseDto update(Long id, CarUpdateRequestDto dto);

    void delete(Long id);

    Car getAvailableCarForRental(Long carId);

    void changeStatus(Long id, CarStatus status);
}
