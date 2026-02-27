package com.example.carrental.repository;

import com.example.carrental.entity.Location;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    @EntityGraph(attributePaths = "phones")
    @Override
    Optional<Location> findById(Long id);

    @EntityGraph(attributePaths = "phones")
    @Override
    List<Location> findAll();

    Optional<Location> findByAddressAndCity(String address, String city);
}
