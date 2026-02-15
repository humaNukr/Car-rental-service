package com.example.carrental.listener;

import com.example.carrental.event.PaymentReceivedEvent;
import com.example.carrental.event.RentalCreatedEvent;
import com.example.carrental.repository.PaymentRepository;
import com.example.carrental.repository.RentalRepository;
import com.example.carrental.service.interfaces.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminTelegramListener {

    private final TelegramNotificationService telegramNotificationService;
    private final RentalRepository rentalRepository;
    private final PaymentRepository paymentRepository;

    @EventListener
    @Transactional(readOnly = true)
    public void handleRentalCreated(RentalCreatedEvent event) {
        rentalRepository.findById(event.rentalId()).ifPresent(rental -> {
            String message = String.format(
                    "🚗 Нова оренда створена!\nID Оренди: %d\nАвто: %s %s\nКлієнт: %s",
                    rental.getId(),
                    rental.getCar().getBrand(),
                    rental.getCar().getModel(),
                    rental.getUser().getEmail()
            );
            telegramNotificationService.sendNotification(message);
        });
    }

    @EventListener
    @Transactional(readOnly = true)
    public void handlePaymentReceived(PaymentReceivedEvent event) {
        paymentRepository.findById(event.paymentId()).ifPresent(payment -> {
            String message = String.format(
                    "💰 Оплату отримано!\nОренда ID: %d\nСума: %s $\nКлієнт: %s",
                    payment.getRental().getId(),
                    payment.getAmount(),
                    payment.getRental().getUser().getEmail()
            );
            telegramNotificationService.sendNotification(message);
        });
    }
}