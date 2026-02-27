package com.example.carrental.service.impl;

import com.example.carrental.dto.location.LocationRequestDto;
import com.example.carrental.dto.location.LocationResponseDto;
import com.example.carrental.dto.location.LocationUpdateRequestDto;
import com.example.carrental.entity.Location;
import com.example.carrental.exception.base.EntityNotFoundException;
import com.example.carrental.mapper.location.LocationMapper;
import com.example.carrental.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

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
class LocationServiceImplTest {

    @Mock
    private LocationRepository locationRepository;

    @Spy
    private final LocationMapper locationMapper = Mappers.getMapper(LocationMapper.class);

    @InjectMocks
    private LocationServiceImpl locationService;

    private Location testLocation;

    @BeforeEach
    void setUp() {
        testLocation = createTestLocation();
    }

    @Nested
    @DisplayName("getById() tests")
    class GetByIdTests {
        @Test
        @DisplayName("Should return LocationResponseDto when location exists")
        void shouldReturnDtoWhenLocationExists() {
            when(locationRepository.findById(1L)).thenReturn(Optional.of(testLocation));

            LocationResponseDto result = locationService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getCity()).isEqualTo("Kyiv");
            verify(locationRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when location does not exist")
        void shouldThrowExceptionWhenLocationDoesNotExist() {
            when(locationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> locationService.getById(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Location not found");
        }
    }

    @Nested
    @DisplayName("Create tests")
    class CreateTests {
        @Test
        @DisplayName("Should create new location and return Dto")
        void shouldSaveAndReturnDto() {
            LocationRequestDto requestDto = createLocationRequestDto();
            when(locationRepository.save(any(Location.class))).thenReturn(testLocation);

            LocationResponseDto result = locationService.create(requestDto);

            assertThat(result).isNotNull();
            assertThat(result.getCity()).isEqualTo(requestDto.getCity());
            verify(locationRepository).save(any(Location.class));
            verify(locationMapper).toEntity(requestDto);
        }
    }

    @Nested
    @DisplayName("Update tests")
    class UpdateTests {
        @Test
        @DisplayName("Should update existing location")
        void shouldUpdateAndReturnDto() {
            LocationUpdateRequestDto updateDto = new LocationUpdateRequestDto();
            updateDto.setCity("Lviv");
            updateDto.setAddress("Naukova 1");

            when(locationRepository.findById(1L)).thenReturn(Optional.of(testLocation));

            LocationResponseDto result = locationService.update(1L, updateDto);

            assertThat(result.getCity()).isEqualTo("Lviv");
            assertThat(result.getAddress()).isEqualTo("Naukova 1");
            verify(locationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete tests")
    class DeleteTests {
        @Test
        @DisplayName("Should successfully delete location when it exists")
        void shouldDeleteAndReturnDto() {
            when(locationRepository.findById(1L)).thenReturn(Optional.of(testLocation));
            locationService.delete(1L);
            verify(locationRepository).findById(1L);
            verify(locationRepository).delete(testLocation);
        }

        @Test
        @DisplayName("Should throw exception when trying to delete non-existent location")
        void shouldThrowExceptionWhenDeletingNonExistent() {
            when(locationRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> locationService.delete(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Location not found");
            verify(locationRepository, never()).delete(any());
        }
    }


    private Location createTestLocation() {
        Location location = new Location();
        location.setId(1L);
        location.setCity("Kyiv");
        location.setAddress("Khreshchatyk 1");
        location.setWorkHours("09:00 - 18:00");
        location.setEmail("kyiv@rental.com");
        location.setPhones(List.of("+380991234567"));
        location.setLatitude(new BigDecimal("50.4501"));
        location.setLongitude(new BigDecimal("30.5234"));
        return location;
    }

    private LocationRequestDto createLocationRequestDto() {
        LocationRequestDto dto = new LocationRequestDto();
        dto.setCity("Kyiv");
        dto.setAddress("Khreshchatyk 1");
        dto.setWorkHours("09:00 - 18:00");
        dto.setEmail("kyiv@rental.com");
        dto.setPhones(List.of("+380991234567"));
        dto.setLatitude(new BigDecimal("50.4501"));
        dto.setLongitude(new BigDecimal("30.5234"));
        return dto;
    }
}