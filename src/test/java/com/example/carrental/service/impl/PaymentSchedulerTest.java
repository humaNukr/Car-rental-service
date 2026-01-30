package com.example.carrental.service.impl;

import com.example.carrental.entity.Payment;
import com.example.carrental.enums.PaymentStatus;
import com.example.carrental.repository.PaymentRepository;
import com.example.carrental.service.PaymentScheduler;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentSchedulerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentScheduler paymentScheduler;

    private MockedStatic<Session> sessionMock;

    @BeforeEach
    void setUp() {
        sessionMock = Mockito.mockStatic(Session.class);
    }

    @AfterEach
    void tearDown() {
        sessionMock.close();
    }

    @Test
    @DisplayName("Check Pending: Should expire old payments")
    void shouldExpireOldPayments() throws Exception {
        Payment oldPayment = new Payment();
        oldPayment.setId(1L);
        oldPayment.setSessionId("sess_old");
        oldPayment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findAllByStatusAndCreatedAtBefore(eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(oldPayment));

        Session stripeSession = mock(Session.class);
        when(stripeSession.getStatus()).thenReturn("open");
        sessionMock.when(() -> Session.retrieve("sess_old")).thenReturn(stripeSession);

        paymentScheduler.checkPendingPayments();

        assertEquals(PaymentStatus.CANCELED, oldPayment.getStatus());
        assertTrue(oldPayment.isDeleted());

        verify(stripeSession).expire();

        verify(paymentRepository).saveAll(anyList());
    }
}