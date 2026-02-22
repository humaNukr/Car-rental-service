package com.example.carrental.validation.validator;

import com.example.carrental.validation.annotation.ValidEnum;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumValidator implements ConstraintValidator<ValidEnum, Enum<?>> {
    private Set<String> acceptedValues;
    private String baseMessage;

    @Override
    public void initialize(ValidEnum annotation) {
        acceptedValues = Arrays.stream(annotation.enumClass().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());
        baseMessage = annotation.message();
    }

    @Override
    public boolean isValid(Enum<?> value, ConstraintValidatorContext context) {
        if (value == null) {
            setNewMessage(context);
            return false;
        }
        boolean valid = acceptedValues.contains(value.name());
        if (!valid) {
            setNewMessage(context);
        }
        return valid;
    }

    private void setNewMessage(ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        String allowedValues = String.join(", ", acceptedValues);
        context.buildConstraintViolationWithTemplate(baseMessage + " " + allowedValues).addConstraintViolation();
    }
}

