package com.example.carrental.service.impl;

import com.example.carrental.domain.LicensePlate;
import com.example.carrental.dto.car.CarDetailsDto;
import com.example.carrental.dto.car.CarRequestDto;
import com.example.carrental.dto.car.CarResponseDto;
import com.example.carrental.dto.car.CarSearchParameters;
import com.example.carrental.entity.Car;
import com.example.carrental.entity.Location;
import com.example.carrental.enums.car.CarStatus;
import com.example.carrental.enums.car.CarType;
import com.example.carrental.event.CarDeletedEvent;
import com.example.carrental.exception.base.EntityNotFoundException;
import com.example.carrental.exception.car.CarUnavailableException;
import com.example.carrental.exception.car.LicensePlateAlreadyExistsException;
import com.example.carrental.mapper.car.CarMapper;
import com.example.carrental.repository.CarRepository;
import com.example.carrental.repository.spec.CarSpecificationBuilder;
import com.example.carrental.service.interfaces.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarServiceImplTest {

    @Mock
    private CarRepository carRepository;
    @Mock
    private CarImageService carImageService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private CarSpecificationBuilder carSpecificationBuilder;
    @Mock
    private LocationService locationService;

    @Spy
    private CarMapper carMapper = Mappers.getMapper(CarMapper.class);

    @InjectMocks
    private CarServiceImpl carService;

    @Captor
    private ArgumentCaptor<Car> carCaptor;

    private Car defaultCar;
    private Location defaultLocation;

    @BeforeEach
    void setUp() {
        defaultLocation = new Location();
        defaultLocation.setId(1L);
        defaultLocation.setCity("Kyiv");

        defaultCar = new Car();
        defaultCar.setId(10L);
        defaultCar.setBrand("BMW");
        defaultCar.setModel("X5");
        defaultCar.setType(CarType.SUV);
        defaultCar.setLicensePlate(new LicensePlate("AA0000AA"));
        defaultCar.setDailyFee(BigDecimal.valueOf(100));
        defaultCar.setStatus(CarStatus.AVAILABLE);
        defaultCar.setCurrentLocation(defaultLocation);
    }

    private CarRequestDto createCarRequestDto() {
        CarRequestDto dto = new CarRequestDto();
        dto.setBrand("BMW");
        dto.setModel("X5");
        dto.setType(CarType.SUV);
        dto.setLicensePlate("AA0000AA");
        dto.setDailyFee(BigDecimal.valueOf(100));
        dto.setColor("Black");
        return dto;
    }

    @Nested
    @DisplayName("Save tests")
    class SaveTests {
        @Test
        @DisplayName("Should successfully save car WITH location")
        void shouldSaveCarWithLocation() {
            CarRequestDto requestDto = createCarRequestDto();
            requestDto.setLocationId(1L);

            when(carRepository.existsByLicensePlate(any(LicensePlate.class))).thenReturn(false);
            when(locationService.getLocationById(1L)).thenReturn(defaultLocation);
            when(carRepository.save(any(Car.class))).thenAnswer(i -> i.getArgument(0));

            CarResponseDto result = carService.save(requestDto);

            assertThat(result).isNotNull();
            assertThat(result.getBrand()).isEqualTo("BMW");

            verify(carRepository).save(carCaptor.capture());
            Car savedCar = carCaptor.getValue();
            assertThat(savedCar.getCurrentLocation()).isEqualTo(defaultLocation);
            assertThat(savedCar.getLicensePlate().value()).isEqualTo("AA0000AA");
        }

        @Test
        @DisplayName("Should successfully save car WITHOUT location")
        void shouldSaveCarWithoutLocation() {
            CarRequestDto requestDto = createCarRequestDto();
            requestDto.setLocationId(null);

            when(carRepository.existsByLicensePlate(any(LicensePlate.class))).thenReturn(false);
            when(carRepository.save(any(Car.class))).thenAnswer(i -> i.getArgument(0));

            carService.save(requestDto);

            verify(locationService, never()).getLocationById(any());
            verify(carRepository).save(carCaptor.capture());
            assertThat(carCaptor.getValue().getCurrentLocation()).isNull();
        }

        @Test
        @DisplayName("Should throw exception if license plate already exists")
        void shouldThrowExceptionIfPlateExists() {
            CarRequestDto requestDto = createCarRequestDto();
            when(carRepository.existsByLicensePlate(any(LicensePlate.class))).thenReturn(true);

            assertThatThrownBy(() -> carService.save(requestDto))
                    .isInstanceOf(LicensePlateAlreadyExistsException.class)
                    .hasMessageContaining("already exists");

            verify(carRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get all tests")
    class GetAllTests {
        @Test
        @DisplayName("Should build spec and return mapped list")
        void shouldReturnMappedList() {
            CarSearchParameters params = new CarSearchParameters(
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null);
            PageRequest pageable = PageRequest.of(0, 10);
            @SuppressWarnings("unchecked")
            Specification<Car> spec = (Specification<Car>) Mockito.mock(Specification.class);
            Page<Car> carPage = new PageImpl<>(List.of(defaultCar));

            when(carSpecificationBuilder.build(params)).thenReturn(spec);
            when(carRepository.findAll(spec, pageable)).thenReturn(carPage);

            List<CarResponseDto> result = carService.getAll(params, pageable);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getBrand()).isEqualTo("BMW");
        }
    }

    @Nested
    @DisplayName("Get details tests")
    class GetDetailsTests {
        @Test
        @DisplayName("Should merge car data and images")
        void shouldReturnCarDetailsWithImages() {
            when(carRepository.findByIdWithPessimisticLock(10L)).thenReturn(Optional.of(defaultCar));
            List<String> images = List.of("url1", "url2");
            when(carImageService.getImagesPaths(10L)).thenReturn(images);

            CarDetailsDto result = carService.getDetails(10L);

            assertThat(result).isNotNull();
            assertThat(result.getBrand()).isEqualTo("BMW");
            assertThat(result.getImageUrls()).containsExactly("url1", "url2");
        }
    }

    @Nested
    @DisplayName("Delete tests")
    class DeleteTests {
        @Test
        @DisplayName("Should delete car and publish event")
        void shouldDeleteAndPublishEvent() {
            when(carRepository.findByIdWithPessimisticLock(10L)).thenReturn(Optional.of(defaultCar));

            carService.delete(10L);

            verify(carRepository).delete(defaultCar);

            ArgumentCaptor<CarDeletedEvent> eventCaptor = ArgumentCaptor.forClass(CarDeletedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().carId()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("Semantic status tests")
    class SemanticStatusTests {

        @Test
        @DisplayName("markAsRented: Should change status to RENTED and remove location")
        void markAsRentedShouldUpdateState() {
            when(carRepository.findByIdWithPessimisticLock(10L)).thenReturn(Optional.of(defaultCar));

            carService.markAsRented(10L);

            assertThat(defaultCar.getStatus()).isEqualTo(CarStatus.RENTED);
            assertThat(defaultCar.getCurrentLocation()).isNull();
            verify(carRepository, never()).save(any());
        }

        @Test
        @DisplayName("markAsRented: Should throw if car is not AVAILABLE")
        void markAsRentedShouldThrowIfNotAvailable() {
            defaultCar.setStatus(CarStatus.UNAVAILABLE);
            when(carRepository.findByIdWithPessimisticLock(10L)).thenReturn(Optional.of(defaultCar));

            assertThatThrownBy(() -> carService.markAsRented(10L))
                    .isInstanceOf(CarUnavailableException.class);
        }

        @Test
        @DisplayName("markAsReturned: Should change status to AVAILABLE and set location")
        void markAsReturnedShouldUpdateState() {
            defaultCar.setStatus(CarStatus.RENTED);
            defaultCar.setCurrentLocation(null);
            Location newLocation = new Location();
            newLocation.setId(2L);
            newLocation.setCity("Lviv");

            when(carRepository.findByIdWithPessimisticLock(10L)).thenReturn(Optional.of(defaultCar));

            carService.markAsReturned(10L, newLocation);

            assertThat(defaultCar.getStatus()).isEqualTo(CarStatus.AVAILABLE);
            assertThat(defaultCar.getCurrentLocation()).isEqualTo(newLocation);
            verify(carRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Image proxy tests")
    class ImageProxyTests {
        @Test
        @DisplayName("Should throw EntityNotFoundException if car doesn't exist before uploading images")
        void uploadImagesShouldCheckIfCarExists() {
            when(carRepository.findByIdWithPessimisticLock(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> carService.uploadImages(99L, List.of()))
                    .isInstanceOf(EntityNotFoundException.class);
            verify(carImageService, never()).uploadImages(any(), any());
        }
    }
}