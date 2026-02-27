package com.example.carrental.service.impl;

import com.example.carrental.dto.location.LocationRequestDto;
import com.example.carrental.dto.location.LocationResponseDto;
import com.example.carrental.dto.location.LocationUpdateRequestDto;
import com.example.carrental.entity.Location;
import com.example.carrental.exception.base.EntityNotFoundException;
import com.example.carrental.mapper.location.LocationMapper;
import com.example.carrental.repository.LocationRepository;
import com.example.carrental.service.interfaces.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;

    @Override
    @Transactional(readOnly = true)
    public LocationResponseDto getById(Long id) {
        Location location = getLocationByIdIfExists(id);
        return locationMapper.toResponseDto(location);
    }

    @Override
    public Location getLocationById(Long id) {
        return getLocationByIdIfExists(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponseDto> getAll() {
        return locationRepository.findAll().stream()
                .map(locationMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public LocationResponseDto create(LocationRequestDto dto) {
        Location location = locationMapper.toEntity(dto);
        return locationMapper.toResponseDto(locationRepository.save(location));
    }

    @Override
    @Transactional
    public LocationResponseDto update(Long id, LocationUpdateRequestDto dto) {
        Location location = getLocationByIdIfExists(id);
        locationMapper.updateLocationFromDto(dto, location);
        return locationMapper.toResponseDto(location);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Location location = getLocationByIdIfExists(id);
        locationRepository.delete(location);
    }

    private Location getLocationByIdIfExists(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Location not found with id: " + id));
    }
}