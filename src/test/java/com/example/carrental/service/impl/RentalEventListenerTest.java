package com.example.carrental.service.impl;

import com.example.carrental.entity.Car;
import com.example.carrental.entity.Rental;
import com.example.carrental.enums.CarStatus;
import com.example.carrental.enums.RentalStatus;
import com.example.carrental.event.PaymentExpiredEvent;
import com.example.carrental.repository.CarRepository;
import com.example.carrental.repository.RentalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalEventListenerTest {

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private RentalEventListener rentalEventListener;

    @Test
    @DisplayName("Success: Should cancel rental and free the car when Payment expires")
    void shouldCancelRentalAndFreeCar() {
        Long rentalId = 1L;
        PaymentExpiredEvent event = new PaymentExpiredEvent(rentalId);

        Car car = new Car();
        car.setId(10L);
        car.setStatus(CarStatus.RENTED);

        Rental rental = new Rental();
        rental.setId(rentalId);
        rental.setStatus(RentalStatus.PENDING);
        rental.setCar(car);

        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));

        rentalEventListener.handlePaymentExpired(event);

        assertEquals(RentalStatus.CANCELED, rental.getStatus());
        verify(rentalRepository).save(rental);

        assertEquals(CarStatus.AVAILABLE, car.getStatus());
        verify(carRepository).save(car);
    }
}