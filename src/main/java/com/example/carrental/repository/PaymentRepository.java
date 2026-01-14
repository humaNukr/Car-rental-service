package com.example.carrental.repository;

import com.example.carrental.entity.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findBySessionId(Long sessionId);

    @EntityGraph(attributePaths = {"rental"})
    List<Payment> findAllByRentalUserId(Long sessionId);

}
