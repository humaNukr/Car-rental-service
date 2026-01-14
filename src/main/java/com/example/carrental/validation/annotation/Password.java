package com.example.carrental.validation.annotation;

import com.example.carrental.validation.validator.PasswordValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = {PasswordValidator.class})
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Password {
    String message() default "Password must be at least 8 characters long, contain at least one digit, " +
            "one lowercase letter, one uppercase letter, one special character (@#$%^&+=), " +
            "and must not contain spaces.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
