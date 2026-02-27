package com.example.carrental.service.interfaces;

import com.example.carrental.dto.location.LocationRequestDto;
import com.example.carrental.dto.location.LocationResponseDto;
import com.example.carrental.dto.location.LocationUpdateRequestDto;
import com.example.carrental.entity.Location;

import java.util.List;

public interface LocationService {
    LocationResponseDto getById(Long id);

    Location getLocationById(Long id);

    List<LocationResponseDto> getAll();

    LocationResponseDto create(LocationRequestDto dto);

    LocationResponseDto update(Long id, LocationUpdateRequestDto dto);

    void delete(Long id);
}
