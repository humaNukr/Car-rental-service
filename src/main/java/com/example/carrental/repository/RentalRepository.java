package com.example.carrental.repository;

import com.example.carrental.entity.Rental;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    @EntityGraph(attributePaths = {"car", "user"})
    List<Rental> findAllByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"car", "user"})
    List<Rental> findAllByActualReturnDateIsNull(Pageable pageable);
}
