package com.example.carrental.service.impl;

import com.example.carrental.domain.CarSpecification;
import com.example.carrental.dto.rental.RentalRequestDto;
import com.example.carrental.dto.rental.RentalResponseDto;
import com.example.carrental.entity.Car;
import com.example.carrental.entity.Location;
import com.example.carrental.entity.Rental;
import com.example.carrental.entity.User;
import com.example.carrental.enums.car.CarClass;
import com.example.carrental.enums.car.CarStatus;
import com.example.carrental.enums.car.FuelType;
import com.example.carrental.enums.car.TransmissionType;
import com.example.carrental.enums.rental.RentalStatus;
import com.example.carrental.event.RentalCreatedEvent;
import com.example.carrental.event.RentalReturnedLateEvent;
import com.example.carrental.exception.car.CarUnavailableException;
import com.example.carrental.mapper.rental.RentalMapper;
import com.example.carrental.repository.RentalRepository;
import com.example.carrental.security.SecurityFacade;
import com.example.carrental.service.interfaces.CarService;
import com.example.carrental.service.interfaces.LocationService;
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
    private LocationService locationService;
    @Mock
    private SecurityFacade securityFacade;

    @Captor
    private ArgumentCaptor<Rental> rentalCaptor;

    private RentalServiceImpl rentalService;
    private User defaultUser;
    private Car defaultCar;
    private Location pickupLocation;
    private Location dropOffLocation;

    @BeforeEach
    void setUp() {
        rentalService = new RentalServiceImpl(
                rentalRepository,
                rentalMapper,
                eventPublisher,
                securityFacade,
                carService,
                locationService
        );

        defaultUser = new User();
        defaultUser.setId(10L);
        defaultUser.setEmail(USER_EMAIL);

        pickupLocation = new Location();
        pickupLocation.setId(1L);
        pickupLocation.setCity("Kyiv");

        dropOffLocation = new Location();
        dropOffLocation.setId(2L);
        dropOffLocation.setCity("Lviv");

        defaultCar = new Car();
        defaultCar.setId(5L);
        defaultCar.setBrand("BMW");
        defaultCar.setStatus(CarStatus.AVAILABLE);
        defaultCar.setDailyFee(BigDecimal.TEN);
        defaultCar.setCarClass(CarClass.STANDARD);

        CarSpecification spec = new CarSpecification();
        spec.setTransmission(TransmissionType.AUTOMATIC);
        spec.setFuelType(FuelType.PETROL);
        spec.setSeatingCapacity(5);
        spec.setDoorsQuantity(4);
        spec.setBagQuantity(2);
        spec.setHasAirConditioning(true);
        defaultCar.setSpecification(spec);
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
            request.setPickupLocationId(pickupLocation.getId());
            request.setDropOffLocationId(dropOffLocation.getId());

            when(carService.getAvailableCarForRental(defaultCar.getId())).thenReturn(defaultCar);
            when(locationService.getLocationById(request.getPickupLocationId())).thenReturn(pickupLocation);
            when(locationService.getLocationById(request.getDropOffLocationId())).thenReturn(dropOffLocation);

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
            assertEquals(RentalStatus.PENDING, savedRental.getStatus());
            assertEquals(defaultUser, savedRental.getUser());
            assertEquals(pickupLocation, savedRental.getPickupLocation());

            verify(carService).markAsRented(defaultCar.getId());
            verify(eventPublisher).publishEvent(any(RentalCreatedEvent.class));
        }

        @Test
        @DisplayName("Fail: Should throw if Car is not AVAILABLE")
        void shouldThrowIfCarNotAvailable() {
            when(securityFacade.getCurrentUser()).thenReturn(defaultUser);

            RentalRequestDto request = new RentalRequestDto();
            request.setCarId(defaultCar.getId());
            request.setPickupLocationId(1L);

            when(carService.getAvailableCarForRental(defaultCar.getId()))
                    .thenThrow(new CarUnavailableException("Car is not available for rental"));

            assertThrows(CarUnavailableException.class, () -> rentalService.createRental(request));

            verify(rentalRepository, never()).save(any());
            verify(carService, never()).markAsRented(any());
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
            rental.setDropOffLocation(dropOffLocation);
            rental.setActualReturnDate(null);

            when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));

            RentalResponseDto result = rentalService.returnCar(rentalId, null);

            assertNotNull(result.getActualReturnDate());

            verify(carService).markAsReturned(defaultCar.getId(), dropOffLocation);
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
            rental.setDropOffLocation(dropOffLocation);
            rental.setActualReturnDate(null);

            when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));

            rentalService.returnCar(rentalId, null);

            verify(carService).markAsReturned(defaultCar.getId(), dropOffLocation);
            verify(eventPublisher).publishEvent(any(RentalReturnedLateEvent.class));
        }
    }

    @Nested
    @DisplayName("Get Rental")
    class GetRental {
        @Test
        @DisplayName("getMyRentalById: Should return rental when user is the owner")
        void shouldReturnRentalWhenUserIsOwner() {
            Long rentalId = 1L;
            Long userId = 100L;

            User currentUser = new User();
            currentUser.setId(userId);
            currentUser.setEmail(USER_EMAIL);

            Rental rental = new Rental();
            rental.setId(rentalId);
            rental.setUser(currentUser);
            rental.setCar(defaultCar);
            rental.setPickupLocation(pickupLocation);
            rental.setDropOffLocation(dropOffLocation);

            when(securityFacade.getCurrentUser()).thenReturn(currentUser);
            when(rentalRepository.findByIdAndUserId(rentalId, userId)).thenReturn(Optional.of(rental));

            RentalResponseDto actualDto = rentalService.getMyRentalById(rentalId);

            assertNotNull(actualDto);
            assertEquals(rentalId, actualDto.getId());
        }
    }
}