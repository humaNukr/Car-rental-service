package com.example.carrental.service.interfaces;

import com.example.carrental.dto.car.CarDetailsDto;
import com.example.carrental.dto.car.CarRequestDto;
import com.example.carrental.dto.car.CarResponseDto;
import com.example.carrental.dto.car.CarSearchParameters;
import com.example.carrental.dto.car.CarUpdateRequestDto;
import com.example.carrental.entity.Car;
import com.example.carrental.entity.Location;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CarService {
    CarResponseDto save(CarRequestDto dto);

    List<CarResponseDto> getAll(CarSearchParameters parameters, Pageable pageable);

    CarDetailsDto getDetails(Long id);

    CarResponseDto update(Long id, CarUpdateRequestDto dto);

    void delete(Long id);

    Car getAvailableCarForRental(Long carId);

    void markAsRented(Long carId);

    void markAsReturned(Long carId, Location location);

    void uploadImages(Long carId, List<MultipartFile> files);

    void setMainImage(Long carId, String imageUrl);

    void deleteImages(Long carId, List<String> imageUrls);
}
