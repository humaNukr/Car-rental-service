package com.example.carrental.event;

import java.math.BigDecimal;

public record RentalReturnedLateEvent(
        Long rentalId,
        long lateDays,
        BigDecimal dailyFee
) {
}
