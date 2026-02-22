package com.example.carrental.mapper.car;

import com.example.carrental.config.MapperConfig;
import com.example.carrental.domain.LicensePlate;
import com.example.carrental.dto.car.CarDetailsDto;
import com.example.carrental.dto.car.CarRequestDto;
import com.example.carrental.dto.car.CarResponseDto;
import com.example.carrental.dto.car.CarUpdateRequestDto;
import com.example.carrental.entity.Car;
import com.example.carrental.entity.CarImage;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(config = MapperConfig.class)
public interface CarMapper {
    CarResponseDto toResponseDto(Car car);

    @AfterMapping
    default void addImageLogic(Car car, @MappingTarget CarResponseDto dto) {
        if (car.getImages() == null || car.getImages().isEmpty()) {
            dto.setMainImageUrl(null);
            return;
        }

        List<CarImage> imageUrls = car.getImages();

        String mainImageUrl = imageUrls.stream()
                .filter(CarImage::isMain)
                .findFirst()
                .map(CarImage::getImageUrl)
                .orElse(null);

        dto.setMainImageUrl(mainImageUrl);

    }

    default LicensePlate mapToLicensePlate(String value) {
        return value != null ? new LicensePlate(value) : null;
    }

    default String mapToString(LicensePlate licensePlate) {
        return licensePlate != null ? licensePlate.value() : null;
    }

    @Mapping(target = "deleted", ignore = true)
    Car toEntity(CarRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCarFromDto(CarUpdateRequestDto dto, @MappingTarget Car car);

    CarDetailsDto toDetailsDto(CarResponseDto car, List<String> imageUrls);
}
