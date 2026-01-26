package com.example.carrental.validation.validator;

import com.example.carrental.dto.rental.RentalRequestDto;
import com.example.carrental.validation.annotation.ValidRentalPeriod;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


public class RentalPeriodValidator implements ConstraintValidator<ValidRentalPeriod, RentalRequestDto> {
    @Override
    public boolean isValid(RentalRequestDto dto, ConstraintValidatorContext context) {
        if (dto.getRentalDate() == null || dto.getReturnDate() == null) {
            return true;
        }
        return !dto.getReturnDate().isBefore(dto.getRentalDate());
    }
}

