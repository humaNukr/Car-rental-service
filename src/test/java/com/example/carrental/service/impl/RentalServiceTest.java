package com.example.carrental.service.impl;

import com.example.carrental.dto.rental.RentalRequestDto;
import com.example.carrental.dto.rental.RentalResponseDto;
import com.example.carrental.entity.Car;
import com.example.carrental.entity.Rental;
import com.example.carrental.entity.User;
import com.example.carrental.enums.CarStatus;
import com.example.carrental.event.RentalCreatedEvent;
import com.example.carrental.event.RentalReturnedLateEvent;
import com.example.carrental.exception.car.CarUnavailableException;
import com.example.carrental.mapper.rental.RentalMapper;
import com.example.carrental.repository.RentalRepository;
import com.example.carrental.security.SecurityFacade;
import com.example.carrental.service.interfaces.CarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    private final RentalMapper rentalMapper = Mappers.getMapper(RentalMapper.class);
    private final String USER_EMAIL = "test@user.com";

    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private CarService carService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private SecurityFacade securityFacade;

    @Captor
    private ArgumentCaptor<Rental> rentalCaptor;

    private RentalServiceImpl rentalService;
    private User defaultUser;
    private Car defaultCar;

    @BeforeEach
    void setUp() {
        rentalService = new RentalServiceImpl(
                rentalRepository,
                rentalMapper,
                eventPublisher,
                securityFacade,
                carService
        );

        defaultUser = new User();
        defaultUser.setId(10L);
        defaultUser.setEmail(USER_EMAIL);

        defaultCar = new Car();
        defaultCar.setId(5L);
        defaultCar.setBrand("BMW");
        defaultCar.setStatus(CarStatus.AVAILABLE);
        defaultCar.setDailyFee(BigDecimal.TEN);
    }

    @Nested
    @DisplayName("Create Rental")
    class CreateRental {

        @Test
        @DisplayName("Success: Should calculate fields, save rental, update car status and publish event")
        void shouldCreateRentalSuccessfully() {
            when(securityFacade.getCurrentUser()).thenReturn(defaultUser);

            RentalRequestDto request = new RentalRequestDto();
            request.setCarId(defaultCar.getId());
            request.setRentalDate(LocalDate.now());
            request.setReturnDate(LocalDate.now().plusDays(3));
            when(carService.getAvailableCarForRental(defaultCar.getId())).thenReturn(defaultCar);

            when(rentalRepository.save(any(Rental.class))).thenAnswer(i -> {
                Rental r = i.getArgument(0);
                r.setId(99L);
                return r;
            });

            RentalResponseDto result = rentalService.createRental(request);

            assertEquals(defaultCar.getBrand(), result.getCarBrand());
            assertEquals(defaultUser.getId(), result.getUserId());

            verify(rentalRepository).save(rentalCaptor.capture());
            Rental savedRental = rentalCaptor.getValue();

            assertNull(savedRental.getActualReturnDate());
            assertEquals(defaultUser, savedRental.getUser());

            verify(carService).changeStatus(defaultCar.getId(), CarStatus.RENTED);
            verify(eventPublisher).publishEvent(any(RentalCreatedEvent.class));
        }

        @Test
        @DisplayName("Fail: Should throw if Car is not AVAILABLE")
        void shouldThrowIfCarNotAvailable() {
            when(securityFacade.getCurrentUser()).thenReturn(defaultUser);

            RentalRequestDto request = new RentalRequestDto();
            request.setCarId(defaultCar.getId());

            when(carService.getAvailableCarForRental(defaultCar.getId()))
                    .thenThrow(new CarUnavailableException("Car is not available for rental"));

            assertThrows(CarUnavailableException.class, () -> rentalService.createRental(request));

            verify(rentalRepository, never()).save(any());
            verify(carService, never()).changeStatus(any(), any());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("Return Rental")
    class ReturnRental {

        @Test
        @DisplayName("Success: Should return car WITHOUT fine if returned ON TIME")
        void shouldReturnCarSuccessfullyOnTime() {
            Long rentalId = 100L;
            defaultCar.setStatus(CarStatus.RENTED);

            Rental rental = new Rental();
            rental.setId(rentalId);
            rental.setCar(defaultCar);
            rental.setReturnDate(LocalDate.now());
            rental.setActualReturnDate(null);

            when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));

            RentalResponseDto result = rentalService.returnCar(rentalId);

            assertNotNull(result.getActualReturnDate());

            verify(carService).changeStatus(defaultCar.getId(), CarStatus.AVAILABLE);
            verify(eventPublisher, never()).publishEvent(any(RentalReturnedLateEvent.class));
        }

        @Test
        @DisplayName("Success: Should publish LateReturnEvent if returned LATE")
        void shouldIssueFineWhenReturnedLate() {
            Long rentalId = 100L;
            defaultCar.setStatus(CarStatus.RENTED);
            defaultCar.setDailyFee(BigDecimal.TEN);

            Rental rental = new Rental();
            rental.setId(rentalId);
            rental.setCar(defaultCar);
            rental.setReturnDate(LocalDate.now().minusDays(2));
            rental.setActualReturnDate(null);

            when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));

            rentalService.returnCar(rentalId);

            verify(carService).changeStatus(defaultCar.getId(), CarStatus.AVAILABLE);
            verify(eventPublisher).publishEvent(any(RentalReturnedLateEvent.class));
        }
    }

    @Nested
    @DisplayName("Get Rental")
    class GetRental {

        @Test
        @DisplayName("getMyRentalById: Should return rental when user is the owner")
        void getMyRentalById_ShouldReturnRental_WhenUserIsOwner() {
            Long rentalId = 1L;
            Long userId = 100L;

            User currentUser = new User();
            currentUser.setId(userId);
            currentUser.setEmail(USER_EMAIL);

            Rental rental = new Rental();
            rental.setId(rentalId);
            rental.setUser(currentUser);

            when(securityFacade.getCurrentUser()).thenReturn(currentUser);
            when(rentalRepository.findByIdAndUserId(rentalId, userId)).thenReturn(Optional.of(rental));

            RentalResponseDto actualDto = rentalService.getMyRentalById(rentalId);

            assertNotNull(actualDto);
            assertEquals(rentalId, actualDto.getId());
        }
    }
}