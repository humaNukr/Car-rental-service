package com.example.carrental.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PaymentExpiredEvent {
    private final Long rentalId;
}