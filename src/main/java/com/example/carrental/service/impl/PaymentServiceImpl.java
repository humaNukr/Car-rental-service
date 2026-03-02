package com.example.carrental.service.impl;

import com.example.carrental.dto.payment.CreateFineDto;
import com.example.carrental.dto.payment.PaymentRequestDto;
import com.example.carrental.dto.payment.PaymentResponseDto;
import com.example.carrental.dto.payment.PaymentSessionInfoDto;
import com.example.carrental.entity.Payment;
import com.example.carrental.entity.Rental;
import com.example.carrental.entity.User;
import com.example.carrental.enums.payment.PaymentStatus;
import com.example.carrental.enums.payment.PaymentType;
import com.example.carrental.enums.rental.RentalStatus;
import com.example.carrental.enums.user.UserRole;
import com.example.carrental.event.PaymentReceivedEvent;
import com.example.carrental.exception.base.EntityNotFoundException;
import com.example.carrental.mapper.payment.PaymentMapper;
import com.example.carrental.repository.PaymentRepository;
import com.example.carrental.repository.RentalRepository;
import com.example.carrental.security.SecurityFacade;
import com.example.carrental.service.interfaces.PaymentGateway;
import com.example.carrental.service.interfaces.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final RentalRepository rentalRepository;
    private final PaymentMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    private final PaymentGateway paymentGateway;
    private final SecurityFacade securityFacade;

    @Override
    @Transactional
    public PaymentResponseDto createPaymentSession(PaymentRequestDto requestDto) {
        Rental rental = rentalRepository.findById(requestDto.getRentalId())
                .orElseThrow(() -> new EntityNotFoundException("Rental not found"));

        User currentUser = securityFacade.getCurrentUser();
        if (!rental.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.MANAGER) {
            throw new AccessDeniedException("You cannot create a payment for another user's rental");
        }

        Payment paymentToProcess;
        BigDecimal amountToPay;

        if (requestDto.getType() == PaymentType.PAYMENT) {
            amountToPay = rental.calculateTotalCost();

            paymentToProcess = new Payment();
            paymentToProcess.setRental(rental);
            paymentToProcess.setType(PaymentType.PAYMENT);
            paymentToProcess.setAmount(amountToPay);
            paymentToProcess.setStatus(PaymentStatus.PENDING);

        } else {
            paymentToProcess = paymentRepository.findByRentalIdAndStatusAndType(
                    rental.getId(), PaymentStatus.PENDING, PaymentType.FINE
            ).orElseThrow(() -> new EntityNotFoundException("No pending fines found for this rental"));

            amountToPay = paymentToProcess.getAmount();
        }

        if (paymentToProcess.getSessionId() != null) {
            paymentGateway.expireSession(paymentToProcess.getSessionId());
        }

        PaymentSessionInfoDto sessionInfo = paymentGateway.createSession(
                amountToPay, "car " + rental.getCar().getModel(), requestDto.getType()
        );

        paymentToProcess.setSessionUrl(sessionInfo.sessionUrl());
        paymentToProcess.setSessionId(sessionInfo.sessionId());

        return mapper.toDto(paymentRepository.save(paymentToProcess));
    }

    @Override
    @Transactional
    public PaymentResponseDto handlePaymentSuccess(String sessionId) {
        Payment payment = paymentRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found for session: " + sessionId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("Payment for session {} is already processed. Skipping.", sessionId);
            return mapper.toDto(payment);
        }

        if (paymentGateway.isPaymentSuccessful(sessionId)) {
            payment.setStatus(PaymentStatus.PAID);
            Rental rental = payment.getRental();
            rental.setStatus(RentalStatus.PAID);

            Payment savedPayment = paymentRepository.save(payment);
            eventPublisher.publishEvent(new PaymentReceivedEvent(savedPayment.getId()));

            return mapper.toDto(savedPayment);
        } else {
            throw new RuntimeException("Payment is not completed yet");
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

    @Override
    @Transactional
    public PaymentResponseDto createFine(Long rentalId, CreateFineDto fineDto) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new EntityNotFoundException("Rental not found"));

        Payment fine = new Payment();
        fine.setRental(rental);
        fine.setStatus(PaymentStatus.PENDING);
        fine.setType(PaymentType.FINE);
        fine.setAmount(fineDto.amount());

        return mapper.toDto(paymentRepository.save(fine));
    }
}