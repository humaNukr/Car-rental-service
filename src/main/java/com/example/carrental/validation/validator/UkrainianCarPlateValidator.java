package com.example.carrental.validation.validator;

import com.example.carrental.validation.annotation.UkrainianCarPlate;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UkrainianCarPlateValidator implements ConstraintValidator<UkrainianCarPlate, String> {
    private static final String TYPE1_15 = "^[АВСЕНІКМОРТХ]{2}\\d{4}[АВСЕНІКМОРТХ]{2}$";
    private static final String TYPE4 = "^[АВСЕНІКМОРТХD]{2}\\d{4}[АВСЕНІКМОРТХD]{2}$";
    private static final String TYPE9_10 = "^[АВСЕНІКМОРТСФХЧЮЯ]{2}\\d{4}[АВСЕНІКМОРТСФХЧЮЯ]{2}$";

    private static final String DIGITS = "^\\d{1,8}$";
    private static final String LETTERS = "^[A-ZА-ЯІЇЄҐ]{1,8}$";
    private static final String MIXED = "^[A-ZА-ЯІЇЄҐ0-9]{1,8}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        return value.matches(TYPE1_15)
                || value.matches(TYPE4)
                || value.matches(TYPE9_10)
                || value.matches(DIGITS)
                || value.matches(LETTERS)
                || value.matches(MIXED);
    }

}
