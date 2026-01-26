package com.example.carrental.service.impl;

import com.example.carrental.dto.car.CarRequestDto;
import com.example.carrental.dto.car.CarResponseDto;
import com.example.carrental.dto.car.CarSearchParameters;
import com.example.carrental.entity.Car;
import com.example.carrental.exception.base.EntityNotFoundException;
import com.example.carrental.exception.car.LicensePlateAlreadyExistsException;
import com.example.carrental.mapper.car.CarMapper;
import com.example.carrental.repository.CarRepository;
import com.example.carrental.repository.spec.CarSpecificationBuilder;
import com.example.carrental.service.CarService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarServiceImpl implements CarService {
    private final CarRepository carRepository;
    private final CarMapper carMapper;
    private final CarSpecificationBuilder carSpecificationBuilder;


    @Override
    public CarResponseDto save(CarRequestDto dto) {
        if (carRepository.existsByLicensePlate(dto.getLicensePlate())) {
            throw new LicensePlateAlreadyExistsException("Car with this license plate already exists");
        }

        Car car = carMapper.toEntity(dto);
        return carMapper.toDto(carRepository.save(car));
    }

    @Override
    public List<CarResponseDto> getAll(CarSearchParameters parameters, Pageable pageable) {
        Specification<Car> spec = carSpecificationBuilder.build(parameters);

        return carRepository.findAll(spec, pageable)
                .stream()
                .map(carMapper::toDto)
                .toList();
    }

    @Override
    public CarResponseDto getById(Long id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Car not found with id: " + id));
        return carMapper.toDto(car);
    }

    @Override
    public CarResponseDto update(Long id, CarRequestDto dto) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Car not found with id: " + id));
        carMapper.updateCarFromDto(dto, car);
        return carMapper.toDto(carRepository.save(car));
    }

    @Override
    public void delete(Long id) {
        if(!carRepository.existsById(id)) {
            throw new EntityNotFoundException("Car not found with id: " + id);
        }
        carRepository.deleteById(id);
    }
}
