package com.example.carrental.service.impl;

import com.example.carrental.dto.rental.RentalRequestDto;
import com.example.carrental.dto.rental.RentalResponseDto;
import com.example.carrental.dto.rental.RentalUpdateRequestDto;
import com.example.carrental.entity.Car;
import com.example.carrental.entity.Rental;
import com.example.carrental.entity.User;
import com.example.carrental.enums.CarStatus;
import com.example.carrental.mapper.rental.RentalMapper;
import com.example.carrental.repository.CarRepository;
import com.example.carrental.repository.RentalRepository;
import com.example.carrental.repository.UserRepository;
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

    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private CarRepository carRepository;
    @Mock
    private UserRepository userRepository;

    private final RentalMapper rentalMapper = Mappers.getMapper(RentalMapper.class);

    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @Captor
    private ArgumentCaptor<Rental> rentalCaptor;

    private RentalServiceImpl rentalService;

    private User defaultUser;
    private Car defaultCar;
    private final String USER_EMAIL = "test@user.com";

    @BeforeEach
    void setUp() {
        rentalService = new RentalServiceImpl(
                rentalRepository,
                carRepository,
                userRepository,
                rentalMapper
        );

        defaultUser = new User();
        defaultUser.setId(10L);
        defaultUser.setEmail(USER_EMAIL);

        defaultCar = new Car();
        defaultCar.setId(5L);
        defaultCar.setBrand("BMW");
        defaultCar.setStatus(CarStatus.AVAILABLE);
        defaultCar.setDailyFee(java.math.BigDecimal.TEN);
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
            request.setRentalDate(LocalDate.now());
            request.setReturnDate(LocalDate.now().plusDays(1));


            assertThrows(RuntimeException.class, () -> rentalService.createRental(request));
            verify(rentalRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Return Rental")
    class ReturnRental {

        @Test
        @DisplayName("Success: Should set actual return date and free the car")
        void shouldReturnCarSuccessfully() {
            Long rentalId = 100L;
            defaultCar.setStatus(CarStatus.RENTED);

            Rental rental = new Rental();
            rental.setId(rentalId);
            rental.setCar(defaultCar);
            rental.setRentalDate(LocalDate.now().minusDays(2));
            rental.setReturnDate(LocalDate.now().plusDays(2));
            rental.setActualReturnDate(null);

            when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));
            when(rentalRepository.save(any(Rental.class))).thenAnswer(i -> i.getArgument(0));

            RentalResponseDto result = rentalService.returnCar(rentalId);

            assertNotNull(result.getActualReturnDate());
            assertEquals(LocalDate.now(), result.getActualReturnDate());

            assertEquals(CarStatus.AVAILABLE, defaultCar.getStatus());
        }

        @Test
        @DisplayName("Fail: Should throw if rental is already finished")
        void shouldThrowIfAlreadyReturned() {
            Rental rental = new Rental();
            rental.setActualReturnDate(LocalDate.now()); // Вже закрита

            when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));

            assertThrows(RuntimeException.class, () -> rentalService.returnCar(1L));
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
    }


    private void mockSecurityContext(String email) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        SecurityContextHolder.setContext(securityContext);
    }
}