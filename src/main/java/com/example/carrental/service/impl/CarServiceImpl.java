package com.example.carrental.service.impl;

import com.example.carrental.domain.LicensePlate;
import com.example.carrental.dto.car.CarDetailsDto;
import com.example.carrental.dto.car.CarRequestDto;
import com.example.carrental.dto.car.CarResponseDto;
import com.example.carrental.dto.car.CarSearchParameters;
import com.example.carrental.dto.car.CarUpdateRequestDto;
import com.example.carrental.entity.Car;
import com.example.carrental.entity.Location;
import com.example.carrental.enums.car.CarStatus;
import com.example.carrental.event.CarDeletedEvent;
import com.example.carrental.exception.base.EntityNotFoundException;
import com.example.carrental.exception.car.CarUnavailableException;
import com.example.carrental.exception.car.LicensePlateAlreadyExistsException;
import com.example.carrental.mapper.car.CarMapper;
import com.example.carrental.repository.CarRepository;
import com.example.carrental.repository.spec.CarSpecificationBuilder;
import com.example.carrental.service.interfaces.CarService;
import com.example.carrental.service.interfaces.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarServiceImpl implements CarService {
    private final CarRepository carRepository;
    private final CarMapper carMapper;
    private final CarImageService carImageService;
    private final ApplicationEventPublisher eventPublisher;
    private final CarSpecificationBuilder carSpecificationBuilder;
    private final LocationService locationService;

    @Override
    @Transactional
    public CarResponseDto save(CarRequestDto dto) {
        checkForPlatesConflict(new LicensePlate(dto.getLicensePlate()));
        Car car = carMapper.toEntity(dto);

        if (dto.getLocationId() != null) {
            Location location = locationService.getLocationById(dto.getLocationId());
            car.setCurrentLocation(location);
        }

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
    public CarDetailsDto getDetails(Long id) {
        Car car = getCarByIdIfExists(id);
        List<String> imageUrls = carImageService.getImagesPaths(id);
        return carMapper.toDetailsDto(car, imageUrls);
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
    public void markAsRented(Long carId) {
        Car car = getCarByIdIfExists(carId);

        if (car.getStatus() != CarStatus.AVAILABLE) {
            throw new CarUnavailableException("Car is not available for rental");
        }

        car.setStatus(CarStatus.RENTED);
        car.setCurrentLocation(null);
    }

    @Override
    @Transactional
    public void markAsReturned(Long carId, Location location) {
        Car car = getCarByIdIfExists(carId);
        car.setStatus(CarStatus.AVAILABLE);
        car.setCurrentLocation(location);
    }

    @Override
    public void uploadImages(Long carId, List<MultipartFile> files) {
        getCarByIdIfExists(carId);
        carImageService.uploadImages(carId, files);
    }

    @Override
    public void setMainImage(Long carId, String imageUrl) {
        getCarByIdIfExists(carId);
        carImageService.setMainImage(carId, imageUrl);
    }

    @Override
    public void deleteImages(Long carId, List<String> imageUrls) {
        getCarByIdIfExists(carId);
        carImageService.deleteImages(carId, imageUrls);
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
