package com.example.carrental.service.impl;

import com.example.carrental.entity.Car;
import com.example.carrental.enums.CarStatus;
import com.example.carrental.enums.RentalStatus;
import com.example.carrental.event.PaymentExpiredEvent;
import com.example.carrental.repository.CarRepository;
import com.example.carrental.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RentalEventListener {

    private final RentalRepository rentalRepository;
    private final CarRepository carRepository;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentExpired(PaymentExpiredEvent event) {
        log.info("Received PaymentExpiredEvent for rental ID: {}", event.getRentalId());
        rentalRepository.findById(event.getRentalId()).ifPresent(rental -> {
            if (rental.getStatus() == RentalStatus.PENDING) {

                rental.setStatus(RentalStatus.CANCELED);
                rentalRepository.save(rental);

                Car car = rental.getCar();
                car.setStatus(CarStatus.AVAILABLE);
                carRepository.save(car);

                log.info("Rental {} canceled automatically. Car {} is now AVAILABLE.", rental.getId(), car.getId());
            }
        });
    }
}