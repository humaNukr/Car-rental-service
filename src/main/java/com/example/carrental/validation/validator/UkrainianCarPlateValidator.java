package com.example.carrental.validation.validator;

import com.example.carrental.validation.annotation.UkrainianCarPlate;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UkrainianCarPlateValidator implements ConstraintValidator<UkrainianCarPlate, String> {
    private static final String PLATE_REGEX = """
            ^(?:[ABCEHIKMOPTX]{2}\\d{4}[ABCEHIKMOPTX]{2}
            |\\d{1,8}
            |[A-Z]{1,8}
            |[A-Z0-9]{1,8})$
            """;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value.matches(PLATE_REGEX);
    }
}
