package com.example.carrental.validation.validator;

import com.example.carrental.domain.LicensePlate;
import com.example.carrental.validation.annotation.UkrainianCarPlate;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UkrainianCarPlateValidator implements ConstraintValidator<UkrainianCarPlate, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return LicensePlate.isValid(value);
    }
}
