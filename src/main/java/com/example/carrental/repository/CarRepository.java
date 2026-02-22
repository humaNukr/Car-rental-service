package com.example.carrental.repository;

import com.example.carrental.domain.LicensePlate;
import com.example.carrental.entity.Car;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CarRepository extends JpaRepository<Car, Long>, JpaSpecificationExecutor<Car> {
    @EntityGraph(attributePaths = "images")
    @Override
    Optional<Car> findById(Long id);

    @EntityGraph(attributePaths = "images")
    @Override
    Page<Car> findAll(Specification<Car> spec, Pageable pageable);

    boolean existsByLicensePlate(LicensePlate licensePlate);
}
