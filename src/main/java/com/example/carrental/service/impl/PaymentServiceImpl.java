package com.example.carrental.service.impl;

import com.example.carrental.config.StripeProperties;
import com.example.carrental.dto.payment.PaymentRequestDto;
import com.example.carrental.dto.payment.PaymentResponseDto;
import com.example.carrental.entity.Payment;
import com.example.carrental.entity.Rental;
import com.example.carrental.enums.PaymentStatus;
import com.example.carrental.enums.PaymentType;
import com.example.carrental.exception.base.EntityNotFoundException;
import com.example.carrental.mapper.payment.PaymentMapper;
import com.example.carrental.repository.PaymentRepository;
import com.example.carrental.repository.RentalRepository;
import com.example.carrental.service.NotificationService;
import com.example.carrental.service.PaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;

    private final RentalRepository rentalRepository;

    private final PaymentMapper mapper;

    private final StripeProperties stripeProperties;

    private final NotificationService notificationService;

    @Override
    @Transactional
    public PaymentResponseDto createPaymentSession(PaymentRequestDto requestDto) {
        Rental rental = rentalRepository.findById(requestDto.getRentalId())
                .orElseThrow(() -> new EntityNotFoundException("Rental not found"));

        paymentRepository.findByRentalIdAndStatus(rental.getId(), PaymentStatus.PENDING)
                .ifPresent(existingPayment -> {
                    try {
                        Session session = Session.retrieve(existingPayment.getSessionId());
                        session.expire();
                    } catch (StripeException e) {
                        log.warn("Session already expired or not found: {}", e.getMessage());
                    }

                    existingPayment.setStatus(PaymentStatus.CANCELED);
                    existingPayment.setDeleted(true);
                    paymentRepository.save(existingPayment);
                });

        BigDecimal amountToPay = calculateAmount(rental, requestDto);

        Session session;
        try {
            session = createStripeSession(amountToPay, rental.getCar().getModel(), requestDto.getType());
        } catch (StripeException e) {
            throw new RuntimeException("Can't create Stripe session", e);
        }

        Payment payment = createPayment(
                rental, session.getUrl(), session.getId(),
                amountToPay, PaymentStatus.PENDING, requestDto.getType()
        );

        return mapper.toDto(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public PaymentResponseDto handlePaymentSuccess(String sessionId) {
        Payment payment = paymentRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found for session: " + sessionId));
        try {
            Session session = Session.retrieve(sessionId);

            if ("paid".equals(session.getPaymentStatus())) {

                payment.setStatus(PaymentStatus.PAID);

                String message = String.format(
                        "Payment received!\nRental ID: %d\nAmount: %s $\nUser: %s",
                        payment.getRental().getId(),
                        payment.getAmount(),
                        payment.getRental().getUser().getEmail()
                );
                notificationService.sendNotification(message);

                return mapper.toDto(paymentRepository.save(payment));
            } else {
                throw new RuntimeException("Payment is not completed yet");
            }

        } catch (StripeException e) {
            throw new RuntimeException("Error verifying payment with Stripe", e);
        }
    }

    @Override
    @Transactional
    public void handlePaymentCancel(String sessionId) {
        Payment payment = paymentRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found for session: " + sessionId));

        payment.setStatus(PaymentStatus.CANCELED);
        payment.setDeleted(true);
        paymentRepository.save(payment);
    }


    private Session createStripeSession(BigDecimal amount, String carModel, PaymentType type)
            throws StripeException {

        long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(stripeProperties.getSuccessUrl() + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(stripeProperties.getCancelUrl())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(type + " for car " + carModel)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        return Session.create(params);
    }


    private Payment createPayment(
            Rental rental, String url, String sessionId,
            BigDecimal amountToPay, PaymentStatus paymentStatus, @NotNull PaymentType type
    ) {
        Payment payment = new Payment();
        payment.setRental(rental);
        payment.setSessionUrl(url);
        payment.setSessionId(sessionId);
        payment.setAmount(amountToPay);
        payment.setStatus(paymentStatus);
        payment.setType(type);
        return payment;
    }


    private BigDecimal calculateAmount(Rental rental, PaymentRequestDto requestDto) {
        if (requestDto.getType() == PaymentType.FINE) {
            if (requestDto.getAmount() == null) {
                throw new IllegalArgumentException("Fine payment amount cannot be null");
            }
            return requestDto.getAmount();
        }

        long days = ChronoUnit.DAYS.between(rental.getRentalDate(), rental.getReturnDate());
        if (days == 0) days = 1;

        return rental.getCar().getDailyFee().multiply(BigDecimal.valueOf(days));
    }
}

