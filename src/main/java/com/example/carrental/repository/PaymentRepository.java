package com.example.carrental.repository;

import com.example.carrental.entity.Payment;
import com.example.carrental.enums.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findBySessionId(String sessionId);

    @EntityGraph(attributePaths = {"rental"})
    List<Payment> findAllByRentalUserId(Long userId);

    Optional<Payment> findByRentalIdAndStatus(Long rentalId, PaymentStatus status);

    List<Payment> findAllByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime dateTime);
}
