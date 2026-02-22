package com.example.carrental.service.impl;

import com.example.carrental.domain.LicensePlate;
import com.example.carrental.dto.car.CarRequestDto;
import com.example.carrental.dto.car.CarResponseDto;
import com.example.carrental.dto.car.CarSearchParameters;
import com.example.carrental.dto.car.CarUpdateRequestDto;
import com.example.carrental.entity.Car;
import com.example.carrental.enums.CarStatus;
import com.example.carrental.event.CarDeletedEvent;
import com.example.carrental.exception.base.EntityNotFoundException;
import com.example.carrental.exception.car.CarUnavailableException;
import com.example.carrental.exception.car.LicensePlateAlreadyExistsException;
import com.example.carrental.mapper.car.CarMapper;
import com.example.carrental.repository.CarRepository;
import com.example.carrental.repository.spec.CarSpecificationBuilder;
import com.example.carrental.service.interfaces.CarService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarServiceImpl implements CarService {
    private final CarRepository carRepository;
    private final CarMapper carMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final CarSpecificationBuilder carSpecificationBuilder;

    @Override
    @Transactional
    public CarResponseDto save(CarRequestDto dto) {
        checkForPlatesConflict(new LicensePlate(dto.getLicensePlate()));
        Car car = carMapper.toEntity(dto);
        return carMapper.toResponseDto(carRepository.save(car));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarResponseDto> getAll(CarSearchParameters parameters, Pageable pageable) {
        Specification<Car> spec = carSpecificationBuilder.build(parameters);

        return carRepository.findAll(spec, pageable)
                .stream()
                .map(carMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CarResponseDto getById(Long id) {
        Car car = getCarByIdIfExists(id);
        return carMapper.toResponseDto(car);
    }

    @Override
    @Transactional
    public CarResponseDto update(Long id, CarUpdateRequestDto dto) {
        Car car = getCarByIdIfExists(id);
        carMapper.updateCarFromDto(dto, car);
        return carMapper.toResponseDto(car);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Car car = getCarByIdIfExists(id);
        carRepository.delete(car);
        eventPublisher.publishEvent(new CarDeletedEvent(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Car getAvailableCarForRental(Long id) {
        Car car = getCarByIdIfExists(id);

        if (car.getStatus() != CarStatus.AVAILABLE) {
            throw new CarUnavailableException("Car is not available for rental");
        }
        return car;
    }

    @Override
    @Transactional
    public void changeStatus(Long id, CarStatus status) {
        Car car = getCarByIdIfExists(id);
        car.setStatus(status);
    }

    private Car getCarByIdIfExists(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Car not found with id: " + id));
    }

    private void checkForPlatesConflict(LicensePlate licensePlate) {
        if (carRepository.existsByLicensePlate(licensePlate)) {
            throw new LicensePlateAlreadyExistsException("Car with this license plate already exists");
        }
    }
}
