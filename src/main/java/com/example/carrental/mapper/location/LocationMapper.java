package com.example.carrental.mapper.location;

import com.example.carrental.config.MapperConfig;
import com.example.carrental.dto.location.LocationRequestDto;
import com.example.carrental.dto.location.LocationResponseDto;
import com.example.carrental.dto.location.LocationUpdateRequestDto;
import com.example.carrental.entity.Location;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = MapperConfig.class)
public interface LocationMapper {
    LocationResponseDto toResponseDto(Location location);

    @Mapping(target = "deleted", ignore = true)
    Location toEntity(LocationRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateLocationFromDto(LocationUpdateRequestDto dto, @MappingTarget Location location);
}
