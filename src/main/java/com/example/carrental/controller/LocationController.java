package com.example.carrental.controller;

import com.example.carrental.dto.location.LocationRequestDto;
import com.example.carrental.dto.location.LocationResponseDto;
import com.example.carrental.dto.location.LocationUpdateRequestDto;
import com.example.carrental.service.interfaces.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {
    private final LocationService locationService;

    @Operation(summary = "Get location by id (permit for all)")
    @GetMapping("/{id}")
    public LocationResponseDto getById(@PathVariable Long id) {
        return locationService.getById(id);
    }

    @GetMapping()
    public List<LocationResponseDto> getAll() {
        return locationService.getAll();
    }

    @PostMapping()
    @PreAuthorize("hasRole('MANAGER')")
    public LocationResponseDto createLocation(@RequestBody @Valid LocationRequestDto dto) {
        return locationService.create(dto);
    }

    @PutMapping("/{id}/update")
    @PreAuthorize("hasRole('MANAGER')")
    public LocationResponseDto updateLocation(@PathVariable Long id, @RequestBody @Valid LocationUpdateRequestDto dto) {
        return locationService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public void deleteLocation(@PathVariable Long id) {
        locationService.delete(id);
    }
}
