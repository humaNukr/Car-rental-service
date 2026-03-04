package com.example.carrental.domain;

import jakarta.persistence.Column;

import java.util.List;
import java.util.regex.Pattern;

public record LicensePlate(
        @Column(name = "license_plate", nullable = false, unique = true)
        String value
) {
    private static final List<Pattern> PATTERNS = List.of(
            Pattern.compile("^[А-ЯA-Z]{2}\\d{4}[А-ЯA-Z]{2}$"),
            Pattern.compile("^[А-ЯA-Z0-9]{1,10}$"),
            Pattern.compile("^[\\p{L}\\p{N}\\s]{3,8}$", Pattern.UNICODE_CHARACTER_CLASS)
    );

    public LicensePlate {
        if (!isValid(value)) {
            throw new IllegalArgumentException("Invalid Ukrainian license plate format: " + value);
        }
    }

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        String sanitized = value.trim().toUpperCase();

        return PATTERNS.stream().anyMatch(p -> p.matcher(sanitized).matches());
    }

    @Override
    public String toString() {
        return value;
    }
}
