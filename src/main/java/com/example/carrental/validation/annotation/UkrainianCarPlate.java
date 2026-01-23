package com.example.carrental.validation.annotation;

import com.example.carrental.validation.validator.UkrainianCarPlateValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = {UkrainianCarPlateValidator.class})
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface UkrainianCarPlate {
    String message() default "Invalid Ukrainian car plate number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
