package com.example.carrental.config;

import com.example.carrental.entity.Payment;
import com.example.carrental.enums.PaymentStatus;
import com.example.carrental.event.PaymentExpiredEvent;
import com.example.carrental.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentScheduler {

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelayString = "${scheduler.fixed-delay}")
    @Transactional
    public void checkPendingPayments() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);

        List<Payment> expiredPayments = paymentRepository.findAllByStatusAndCreatedAtBefore(
                PaymentStatus.PENDING,
                cutoffTime
        );

        if (expiredPayments.isEmpty()) {
            return;
        }

        log.info("Found {} expired payments. Cleaning up...", expiredPayments.size());

        for (Payment payment : expiredPayments) {
            try {
                Session session = Session.retrieve(payment.getSessionId());
                if ("open".equals(session.getStatus())) {
                    session.expire();
                }
            } catch (StripeException ignored) {
            }

            payment.setStatus(PaymentStatus.CANCELED);
            payment.setDeleted(true);

            if (payment.getRental() != null) {
                eventPublisher.publishEvent(new PaymentExpiredEvent(payment.getRental().getId()));
            }
        }

        paymentRepository.saveAll(expiredPayments);
    }
}