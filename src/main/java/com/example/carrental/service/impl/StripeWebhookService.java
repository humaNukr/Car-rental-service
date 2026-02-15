package com.example.carrental.service.impl;

import com.example.carrental.properties.StripeProperties;
import com.example.carrental.service.interfaces.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookService {

    private final StripeProperties stripeProperties;
    private final PaymentService paymentService;

    public void processWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(
                    payload, sigHeader, stripeProperties.getWebhookSecret()
            );
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe signature: {}", e.getMessage());
            throw new RuntimeException("Invalid webhook signature");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

            if (dataObjectDeserializer.getObject().isPresent()) {
                Session session = (Session) dataObjectDeserializer.getObject().get();
                log.info("Webhook received: checkout.session.completed for session {}", session.getId());
                
                paymentService.handlePaymentSuccess(session.getId());
            }
        }
    }
}