package com.example.carrental.listener;

import com.example.carrental.dto.payment.CreateFineDto;
import com.example.carrental.event.RentalReturnedLateEvent;
import com.example.carrental.properties.RentalProperties;
import com.example.carrental.service.interfaces.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class LateReturnFineListener {

    private final PaymentService paymentService;
    private final RentalProperties rentalProperties;

    @EventListener
    public void handleLateReturn(RentalReturnedLateEvent event) {
        log.info("Processing late return fine for rental {}. Late by {} days.", event.rentalId(), event.lateDays());

        BigDecimal multiplier = BigDecimal.valueOf(rentalProperties.getFine().getLateReturnMultiplier());
        BigDecimal fineAmount = event.dailyFee().multiply(BigDecimal.valueOf(event.lateDays())).multiply(multiplier);

        CreateFineDto autoFine = new CreateFineDto(fineAmount, "LATE_RETURN");
        paymentService.createFine(event.rentalId(), autoFine);
    }
}