package com.example.carrental.repository;

import com.example.carrental.entity.Car;
import com.example.carrental.enums.CarStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CarRepository extends JpaRepository<Car, Long>, JpaSpecificationExecutor<Car> {
    Page<Car> findAllByStatus(CarStatus status, Pageable pageable);

    boolean existsByLicensePlate(String licensePlate);
}
