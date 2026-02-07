package com.example.carrental.repository;

import com.example.carrental.entity.Car;
import com.example.carrental.entity.CarImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarImageRepository extends JpaRepository<CarImage, Long> {
    Optional<CarImage> findByCarIdAndIsMainTrue(Long carId);

    Optional<CarImage> findByImageUrl(String imageUrl);

    List<CarImage> findCarImagesByCar(Car car);
}
