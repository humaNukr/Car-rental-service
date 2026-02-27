package com.example.carrental.validation.annotation;

import com.example.carrental.validation.validator.LocationConsistencyValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = {LocationConsistencyValidator.class})
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LocationConsistency {
    String message() default "Address and coordinates must be updated together";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
