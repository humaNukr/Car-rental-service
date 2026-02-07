package com.example.carrental.service.impl;

import com.example.carrental.config.RentalProperties;
import com.example.carrental.dto.payment.CreateFineDto;
import com.example.carrental.dto.rental.RentalRequestDto;
import com.example.carrental.dto.rental.RentalResponseDto;
import com.example.carrental.dto.rental.RentalUpdateRequestDto;
import com.example.carrental.entity.Car;
import com.example.carrental.entity.Rental;
import com.example.carrental.entity.User;
import com.example.carrental.enums.CarStatus;
import com.example.carrental.exception.car.CarUnavailableException;
import com.example.carrental.exception.rental.RentalAlreadyFinishedException;
import com.example.carrental.mapper.rental.RentalMapper;
import com.example.carrental.repository.CarRepository;
import com.example.carrental.repository.RentalRepository;
import com.example.carrental.repository.UserRepository;
import com.example.carrental.service.interfaces.NotificationService;
import com.example.carrental.service.interfaces.PaymentService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private CarRepository carRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @Mock
    private PaymentService paymentService;
    @Mock
    private RentalProperties rentalProperties;

    @Captor
    private ArgumentCaptor<Rental> rentalCaptor;
    @Captor
    private ArgumentCaptor<CreateFineDto> fineCaptor;

    private RentalServiceImpl rentalService;
    private User defaultUser;
    private Car defaultCar;

    @BeforeEach
    void setUp() {
        rentalService = new RentalServiceImpl(
                rentalRepository,
                carRepository,
                userRepository,
                rentalMapper,
                notificationService,
                rentalProperties,
                paymentService
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

    private void mockSecurityContext(String email) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    @DisplayName("Create Rental")
    class CreateRental {

        @Test
        @DisplayName("Success: Should calculate fields, save rental and update car status")
        void shouldCreateRentalSuccessfully() {
            mockSecurityContext(USER_EMAIL);

            RentalRequestDto request = new RentalRequestDto();
            request.setCarId(defaultCar.getId());
            request.setRentalDate(LocalDate.now());
            request.setReturnDate(LocalDate.now().plusDays(3));

            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(defaultUser));
            when(carRepository.findById(defaultCar.getId())).thenReturn(Optional.of(defaultCar));
            when(rentalRepository.save(any(Rental.class))).thenAnswer(i -> i.getArgument(0));

            RentalResponseDto result = rentalService.createRental(request);

            assertEquals(defaultCar.getBrand(), result.getCarBrand());
            assertEquals(defaultUser.getId(), result.getUserId());
            assertEquals(CarStatus.RENTED, defaultCar.getStatus(), "Car status must change to RENTED");

            verify(rentalRepository).save(rentalCaptor.capture());
            Rental savedRental = rentalCaptor.getValue();

            assertNull(savedRental.getActualReturnDate());
            assertEquals(defaultUser, savedRental.getUser());

            verify(notificationService).sendNotification(any(String.class));
        }

        @Test
        @DisplayName("Fail: Should throw if Car is not AVAILABLE")
        void shouldThrowIfCarNotAvailable() {
            mockSecurityContext(USER_EMAIL);
            defaultCar.setStatus(CarStatus.RENTED);

            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(defaultUser));
            when(carRepository.findById(defaultCar.getId())).thenReturn(Optional.of(defaultCar));

            RentalRequestDto request = new RentalRequestDto();
            request.setCarId(defaultCar.getId());

            assertThrows(CarUnavailableException.class, () -> rentalService.createRental(request));
            verify(rentalRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Return Rental")
    class ReturnRental {

        @Test
        @DisplayName("Success: Should return car without fine if returned ON TIME")
        void shouldReturnCarSuccessfullyOnTime() {
            Long rentalId = 100L;
            defaultCar.setStatus(CarStatus.RENTED);

            Rental rental = new Rental();
            rental.setId(rentalId);
            rental.setCar(defaultCar);
            rental.setReturnDate(LocalDate.now());
            rental.setActualReturnDate(null);

            when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));
            when(rentalRepository.save(any(Rental.class))).thenAnswer(i -> i.getArgument(0));

            RentalResponseDto result = rentalService.returnCar(rentalId);

            assertNotNull(result.getActualReturnDate());
            assertEquals(LocalDate.now(), result.getActualReturnDate());
            assertEquals(CarStatus.AVAILABLE, defaultCar.getStatus());

            verify(paymentService, never()).createFine(any(), any());
        }

        @Test
        @DisplayName("Success: Should issue FINE automatically if returned LATE")
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
            when(rentalRepository.save(any(Rental.class))).thenAnswer(i -> i.getArgument(0));

            RentalProperties.Fine fineConfig = new RentalProperties.Fine();
            fineConfig.setLateReturnMultiplier(1.5);
            when(rentalProperties.getFine()).thenReturn(fineConfig);

            rentalService.returnCar(rentalId);

            assertEquals(CarStatus.AVAILABLE, defaultCar.getStatus());

            verify(paymentService).createFine(eq(rentalId), fineCaptor.capture());
            CreateFineDto capturedFine = fineCaptor.getValue();

            assertEquals(0, BigDecimal.valueOf(30.0).compareTo(capturedFine.amount()));
            assertEquals("LATE_RETURN", capturedFine.type());
        }

        @Test
        @DisplayName("Fail: Should throw if rental is already finished")
        void shouldThrowIfAlreadyReturned() {
            Rental rental = new Rental();
            rental.setActualReturnDate(LocalDate.now());

            when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));

            assertThrows(RentalAlreadyFinishedException.class, () -> rentalService.returnCar(1L));
        }
    }

    @Nested
    @DisplayName("Update Rental")
    class UpdateRental {
        @Test
        @DisplayName("Success: Should update only return date")
        void shouldUpdateReturnDate() {
            Long rentalId = 1L;
            Rental rental = new Rental();
            rental.setId(rentalId);
            rental.setRentalDate(LocalDate.now());
            rental.setReturnDate(LocalDate.now().plusDays(2));

            RentalUpdateRequestDto updateDto = new RentalUpdateRequestDto();
            updateDto.setReturnDate(LocalDate.now().plusDays(5));

            when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));
            when(rentalRepository.save(any(Rental.class))).thenAnswer(i -> i.getArgument(0));

            RentalResponseDto result = rentalService.updateRental(rentalId, updateDto);

            assertEquals(updateDto.getReturnDate(), result.getReturnDate());
            assertEquals(rental.getRentalDate(), result.getRentalDate());
        }

        @Test
        @DisplayName("Fail: Should throw exception if rental already finished")
        void shouldThrowIfUpdatingFinishedRental() {
            Long rentalId = 1L;
            Rental rental = new Rental();
            rental.setId(rentalId);
            rental.setActualReturnDate(LocalDate.now());

            when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));

            RentalUpdateRequestDto updateDto = new RentalUpdateRequestDto();
            updateDto.setReturnDate(LocalDate.now().plusDays(5));

            assertThrows(RentalAlreadyFinishedException.class,
                    () -> rentalService.updateRental(rentalId, updateDto));
        }
    }
}