package com.example.carrental.service.impl;

import com.example.carrental.dto.payment.PaymentSessionInfoDto;
import com.example.carrental.enums.payment.PaymentType;
import com.example.carrental.properties.StripeProperties;
import com.example.carrental.service.interfaces.PaymentGateway;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class StripePaymentGateway implements PaymentGateway {

    private final StripeProperties stripeProperties;

    @Override
    public PaymentSessionInfoDto createSession(BigDecimal amount, String description, PaymentType type) {
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
                                                                .setName(type + " for " + description)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        try {
            Session session = Session.create(params);
            return new PaymentSessionInfoDto(session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new RuntimeException("Failed to create Stripe payment session", e);
        }
    }

    @Override
    public boolean isPaymentSuccessful(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);
            return "paid".equals(session.getPaymentStatus());
        } catch (StripeException e) {
            log.error("Error verifying payment with Stripe for session {}", sessionId, e);
            return false;
        }
    }

    @Override
    public void expireSession(String sessionId) {
        try {
            Session.retrieve(sessionId).expire();
        } catch (StripeException e) {
            log.warn("Old session {} expired/not found in Stripe", sessionId);
        }
    }
}