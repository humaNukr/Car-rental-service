package com.example.carrental.mapper.rental;

import com.example.carrental.config.MapperConfig;
import com.example.carrental.dto.rental.RentalRequestDto;
import com.example.carrental.dto.rental.RentalResponseDto;
import com.example.carrental.dto.rental.RentalUpdateRequestDto;
import com.example.carrental.entity.Car;
import com.example.carrental.entity.Rental;
import com.example.carrental.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperConfig.class)
public interface RentalMapper {

    @Mapping(source = "car.id", target = "carId")
    @Mapping(source = "car.brand", target = "carBrand")
    @Mapping(source = "car.model", target = "carModel")
    @Mapping(source = "user.id", target = "userId")
    RentalResponseDto toDto(Rental rental);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "actualReturnDate", ignore = true)
    @Mapping(target = "car", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Rental toEntity(RentalRequestDto requestDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "actualReturnDate", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "rentalDate", source = "requestDto.rentalDate")
    @Mapping(target = "returnDate", source = "requestDto.returnDate")
    @Mapping(target = "car", source = "car")
    @Mapping(target = "user", source = "user")
    Rental toEntity(RentalRequestDto requestDto, Car car, User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "car", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "actualReturnDate", ignore = true)
    void updateRentalFromDto(RentalUpdateRequestDto dto, @MappingTarget Rental rental);
}
