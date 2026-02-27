package com.example.carrental.validation.validator;

import com.example.carrental.dto.location.LocationUpdateRequestDto;
import com.example.carrental.validation.annotation.LocationConsistency;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LocationConsistencyValidator implements ConstraintValidator<LocationConsistency, LocationUpdateRequestDto> {

    @Override
    public boolean isValid(LocationUpdateRequestDto dto, ConstraintValidatorContext context) {
        boolean addressChanged = dto.getAddress() != null && !dto.getAddress().isBlank();
        boolean coordsChanged = dto.getLatitude() != null && dto.getLongitude() != null;
        return (addressChanged && coordsChanged) || (!addressChanged && !coordsChanged);
    }
}
