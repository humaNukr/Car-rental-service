package com.example.carrental.repository;

import com.example.carrental.entity.Car;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarRepository extends JpaRepository<Car, Long> {
    Page<Car> findAllByStatus(String carStatus, Pageable pageable);

    boolean existsByLicensePlate(String licensePlate);
}
