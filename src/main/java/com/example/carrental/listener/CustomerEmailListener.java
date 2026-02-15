package com.example.carrental.listener;

import com.example.carrental.event.PaymentReceivedEvent;
import com.example.carrental.properties.AppProperties;
import com.example.carrental.repository.PaymentRepository;
import com.example.carrental.service.interfaces.EmailSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerEmailListener {

    private final EmailSenderService emailSenderService;
    private final PaymentRepository paymentRepository;
    private final AppProperties appProperties;

    @Async
    @EventListener
    @Transactional(readOnly = true)
    public void handlePaymentReceived(PaymentReceivedEvent event) {
        log.info("Preparing to send email receipt for payment ID: {}", event.paymentId());

        paymentRepository.findById(event.paymentId()).ifPresent(payment -> {

            Map<String, Object> templateModel = new HashMap<>();
            templateModel.put("name", payment.getRental().getUser().getFirstName());
            templateModel.put("amount", payment.getAmount());
            templateModel.put("carBrand", payment.getRental().getCar().getBrand());
            templateModel.put("carModel", payment.getRental().getCar().getModel());
            templateModel.put("rentalId", payment.getRental().getId());
            templateModel.put("rentalDate", payment.getRental().getRentalDate().toString());
            templateModel.put("returnDate", payment.getRental().getReturnDate().toString());
            String detailsUrl = appProperties.getFrontendUrl() + "/rentals/" + payment.getRental().getId();
            templateModel.put("rentalDetailsUrl", detailsUrl);

            String customerEmail = payment.getRental().getUser().getEmail();

            emailSenderService.sendHtmlEmail(
                    customerEmail,
                    "Квитанція: Оплата оренди авто",
                    "payment-receipt",
                    templateModel
            );
        });
    }
}