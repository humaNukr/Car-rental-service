package com.example.carrental.service.impl;

import com.example.carrental.config.StripeProperties;
import com.example.carrental.dto.payment.PaymentRequestDto;
import com.example.carrental.dto.payment.PaymentResponseDto;
import com.example.carrental.entity.Car;
import com.example.carrental.entity.Payment;
import com.example.carrental.entity.Rental;
import com.example.carrental.entity.User;
import com.example.carrental.enums.PaymentStatus;
import com.example.carrental.enums.PaymentType;
import com.example.carrental.mapper.payment.PaymentMapper;
import com.example.carrental.repository.PaymentRepository;
import com.example.carrental.repository.RentalRepository;
import com.example.carrental.service.NotificationService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private NotificationService notificationService;

    StripeProperties props;

    private final PaymentMapper paymentMapper = Mappers.getMapper(PaymentMapper.class);

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private MockedStatic<Session> sessionMock;

    @BeforeEach
    void setUp() {
        props = new StripeProperties();
        props.setSuccessUrl("http://success");
        props.setCancelUrl("http://cancel");
        ReflectionTestUtils.setField(paymentService, "stripeProperties", props);
        ReflectionTestUtils.setField(paymentService, "mapper", paymentMapper);
        sessionMock = Mockito.mockStatic(Session.class);
    }

    @AfterEach
    void tearDown() {
        sessionMock.close();
    }

    @Nested
    @DisplayName("Create Session")
    class CreateSessionTest {

        @Test
        @DisplayName("Should calculate amount and create new payment")
        void shouldCalculateAmountAndCreateNewPayment() {
            Rental rental = createNewRental();
            PaymentRequestDto request = new PaymentRequestDto();
            request.setType(PaymentType.PAYMENT);
            request.setRentalId(1L);

            when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
            when(paymentRepository.findByRentalIdAndStatus(1L, PaymentStatus.PENDING))
                    .thenReturn(Optional.empty());

            Session mockSession = mock(Session.class);
            when(mockSession.getUrl()).thenReturn("https://stripe.url");
            when(mockSession.getId()).thenReturn("sess_123");

            sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockSession);

            when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

            PaymentResponseDto result = paymentService.createPaymentSession(request);

            assertNotNull(result);
            assertEquals("https://stripe.url", result.getSessionUrl());
            assertEquals("sess_123", result.getSessionId());

            assertEquals(BigDecimal.valueOf(200), result.getAmount());
        }

        @Test
        @DisplayName("Create Session: Should cancel existing PENDING payment")
        void shouldCancelExistingPayment() throws StripeException {
            Rental rental = createNewRental();
            PaymentRequestDto request = new PaymentRequestDto();
            request.setType(PaymentType.PAYMENT);
            request.setRentalId(1L);

            Payment oldPayment = new Payment();
            oldPayment.setId(99L);
            oldPayment.setStatus(PaymentStatus.PENDING);
            oldPayment.setSessionId("sess_old");

            when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
            when(paymentRepository.findByRentalIdAndStatus(1L, PaymentStatus.PENDING))
                    .thenReturn(Optional.of(oldPayment));

            Session oldSession = mock(Session.class);
            sessionMock.when(() -> Session.retrieve("sess_old")).thenReturn(oldSession);

            Session newSession = mock(Session.class);
            when(newSession.getUrl()).thenReturn("https://stripe.url");
            when(newSession.getId()).thenReturn("sess_new");
            sessionMock.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(newSession);

            when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

            paymentService.createPaymentSession(request);

            assertEquals(PaymentStatus.CANCELED, oldPayment.getStatus());
            assertTrue(oldPayment.isDeleted());
            verify(oldSession).expire();

            verify(paymentRepository, times(2)).save(any(Payment.class));
        }

        @Test
        @DisplayName("Should mark payment as PAID if Stripe confirms")
        void shouldMarkPaymentAsPaidIfStripeConfirms() {
            String sessionId = "sess_success";

            User user = new User();
            user.setEmail("email@test");

            Rental rental = new Rental();
            rental.setId(1L);
            rental.setUser(user);

            Payment payment = new Payment();
            payment.setStatus(PaymentStatus.PENDING);
            payment.setAmount(BigDecimal.valueOf(100));
            payment.setRental(rental);

            when(paymentRepository.findBySessionId(sessionId)).thenReturn(Optional.of(payment));

            Session session = mock(Session.class);
            when(session.getPaymentStatus()).thenReturn("paid");
            sessionMock.when(() -> Session.retrieve(sessionId)).thenReturn(session);

            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            PaymentResponseDto result = paymentService.handlePaymentSuccess(sessionId);

            assertEquals(PaymentStatus.PAID, result.getStatus());
            assertEquals(BigDecimal.valueOf(100), result.getAmount());
            assertEquals(1L, result.getRentalId());
        }


        @Test
        @DisplayName("Handle Cancel: Should mark payment as CANCELED")
        void shouldMarkPaymentAsCancelledIfStripeConfirms() {
            String sessionId = "sess_cancel";
            Payment payment = new Payment();
            payment.setStatus(PaymentStatus.PENDING);

            when(paymentRepository.findBySessionId(sessionId)).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

            paymentService.handlePaymentCancel(sessionId);

            assertEquals(PaymentStatus.CANCELED, payment.getStatus());
            assertTrue(payment.isDeleted());
        }
    }

    private Rental createNewRental() {
        Car car = new Car();
        car.setDailyFee(BigDecimal.valueOf(100));
        car.setModel("Tesla");

        Rental rental = new Rental();
        rental.setId(1L);
        rental.setCar(car);
        rental.setRentalDate(LocalDate.now());
        rental.setReturnDate(LocalDate.now().plusDays(2));
        return rental;
    }
}