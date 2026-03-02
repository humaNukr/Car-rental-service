package com.example.carrental.service.impl;

import com.example.carrental.dto.payment.CreateFineDto;
import com.example.carrental.dto.payment.PaymentRequestDto;
import com.example.carrental.dto.payment.PaymentResponseDto;
import com.example.carrental.dto.payment.PaymentSessionInfoDto;
import com.example.carrental.entity.Car;
import com.example.carrental.entity.Payment;
import com.example.carrental.entity.Rental;
import com.example.carrental.entity.User;
import com.example.carrental.enums.payment.PaymentStatus;
import com.example.carrental.enums.payment.PaymentType;
import com.example.carrental.enums.user.UserRole;
import com.example.carrental.exception.base.EntityNotFoundException;
import com.example.carrental.mapper.payment.PaymentMapper;
import com.example.carrental.repository.PaymentRepository;
import com.example.carrental.repository.RentalRepository;
import com.example.carrental.security.SecurityFacade;
import com.example.carrental.service.interfaces.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Spy
    private PaymentMapper paymentMapper = Mappers.getMapper(PaymentMapper.class);

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private SecurityFacade securityFacade;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Captor
    private ArgumentCaptor<Payment> paymentCaptor;

    private User defaultUser;
    private Rental defaultRental;

    @BeforeEach
    void setUp() {
        defaultUser = new User();
        defaultUser.setId(1L);
        defaultUser.setEmail("test@email.com");
        defaultUser.setRole(UserRole.CUSTOMER);

        Car car = new Car();
        car.setDailyFee(BigDecimal.valueOf(100));
        car.setModel("Tesla");

        defaultRental = new Rental();
        defaultRental.setId(1L);
        defaultRental.setUser(defaultUser);
        defaultRental.setCar(car);
        defaultRental.setRentalDate(LocalDate.now());
        defaultRental.setReturnDate(LocalDate.now().plusDays(2));
    }

    @Nested
    @DisplayName("Create Fine (Manager)")
    class CreateFineTest {
        @Test
        @DisplayName("Should create PENDING fine in DB")
        void shouldCreateFine() {
            CreateFineDto fineDto = new CreateFineDto(BigDecimal.valueOf(50.0), "DAMAGE");

            when(rentalRepository.findById(1L)).thenReturn(Optional.of(defaultRental));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

            paymentService.createFine(1L, fineDto);

            verify(paymentRepository).save(paymentCaptor.capture());
            Payment saved = paymentCaptor.getValue();

            assertEquals(PaymentType.FINE, saved.getType());
            assertEquals(PaymentStatus.PENDING, saved.getStatus());
            assertEquals(BigDecimal.valueOf(50.0), saved.getAmount());
        }
    }

    @Nested
    @DisplayName("Create Session (User)")
    class CreateSessionTest {

        @Test
        @DisplayName("Should throw AccessDeniedException if user is not owner and not manager")
        void shouldThrowAccessDeniedIfUserNotOwner() {
            PaymentRequestDto request = new PaymentRequestDto();
            request.setRentalId(1L);

            User otherUser = new User();
            otherUser.setId(2L);
            otherUser.setRole(UserRole.CUSTOMER);

            when(rentalRepository.findById(1L)).thenReturn(Optional.of(defaultRental));
            when(securityFacade.getCurrentUser()).thenReturn(otherUser);

            assertThrows(AccessDeniedException.class, () -> paymentService.createPaymentSession(request));
            verify(paymentGateway, never()).createSession(any(), any(), any());
        }

        @Test
        @DisplayName("Should calculate amount and create new payment session for PAYMENT")
        void shouldCalculateAmountAndCreateNewPayment() {
            PaymentRequestDto request = new PaymentRequestDto();
            request.setType(PaymentType.PAYMENT);
            request.setRentalId(1L);

            when(rentalRepository.findById(1L)).thenReturn(Optional.of(defaultRental));
            when(securityFacade.getCurrentUser()).thenReturn(defaultUser);

            PaymentSessionInfoDto mockSessionInfo = new PaymentSessionInfoDto("sess_123", "https://stripe.url");
            when(paymentGateway.createSession(eq(BigDecimal.valueOf(200)), anyString(), eq(PaymentType.PAYMENT)))
                    .thenReturn(mockSessionInfo);

            when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

            PaymentResponseDto result = paymentService.createPaymentSession(request);

            assertNotNull(result);
            assertEquals("https://stripe.url", result.getSessionUrl());
            assertEquals("sess_123", result.getSessionId());
            assertEquals(BigDecimal.valueOf(200), result.getAmount());
            assertEquals(PaymentType.PAYMENT, result.getType());
        }

        @Test
        @DisplayName("Should use existing fine amount from DB for FINE")
        void shouldCreateSessionForExistingFine() {
            PaymentRequestDto request = new PaymentRequestDto();
            request.setType(PaymentType.FINE);
            request.setRentalId(1L);

            Payment existingFine = new Payment();
            existingFine.setId(55L);
            existingFine.setType(PaymentType.FINE);
            existingFine.setStatus(PaymentStatus.PENDING);
            existingFine.setAmount(BigDecimal.valueOf(75.00));
            existingFine.setRental(defaultRental);

            when(rentalRepository.findById(1L)).thenReturn(Optional.of(defaultRental));
            when(securityFacade.getCurrentUser()).thenReturn(defaultUser);
            when(paymentRepository.findByRentalIdAndStatusAndType(1L, PaymentStatus.PENDING, PaymentType.FINE))
                    .thenReturn(Optional.of(existingFine));

            PaymentSessionInfoDto mockSessionInfo = new PaymentSessionInfoDto("sess_fine", "https://stripe.fine");
            when(paymentGateway.createSession(eq(BigDecimal.valueOf(75.00)), anyString(), eq(PaymentType.FINE)))
                    .thenReturn(mockSessionInfo);

            when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

            PaymentResponseDto result = paymentService.createPaymentSession(request);

            assertEquals(BigDecimal.valueOf(75.00), result.getAmount());
            assertEquals("sess_fine", result.getSessionId());
        }

        @Test
        @DisplayName("Should expire old session if payment already has one")
        void shouldExpireOldSession() {
            PaymentRequestDto request = new PaymentRequestDto();
            request.setType(PaymentType.FINE);
            request.setRentalId(1L);

            Payment existingFine = new Payment();
            existingFine.setStatus(PaymentStatus.PENDING);
            existingFine.setType(PaymentType.FINE);
            existingFine.setAmount(BigDecimal.TEN);
            existingFine.setSessionId("sess_old");
            existingFine.setRental(defaultRental);

            when(rentalRepository.findById(1L)).thenReturn(Optional.of(defaultRental));
            when(securityFacade.getCurrentUser()).thenReturn(defaultUser);
            when(paymentRepository.findByRentalIdAndStatusAndType(1L, PaymentStatus.PENDING, PaymentType.FINE))
                    .thenReturn(Optional.of(existingFine));

            PaymentSessionInfoDto mockSessionInfo = new PaymentSessionInfoDto("sess_new", "https://new");
            when(paymentGateway.createSession(any(), anyString(), any())).thenReturn(mockSessionInfo);

            when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

            paymentService.createPaymentSession(request);

            verify(paymentGateway).expireSession("sess_old");
            assertEquals("sess_new", existingFine.getSessionId());
        }
    }

    @Nested
    @DisplayName("Handle Callbacks")
    class HandleCallbacksTest {

        @Test
        @DisplayName("Should mark payment as PAID if gateway confirms")
        void shouldMarkPaymentAsPaidIfGatewayConfirms() {
            Payment payment = new Payment();
            payment.setId(55L);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setAmount(BigDecimal.valueOf(100));
            payment.setRental(defaultRental);
            String sessionId = "sess_success";

            when(paymentRepository.findBySessionId(sessionId)).thenReturn(Optional.of(payment));
            when(paymentGateway.isPaymentSuccessful(sessionId)).thenReturn(true);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            PaymentResponseDto result = paymentService.handlePaymentSuccess(sessionId);

            assertEquals(PaymentStatus.PAID, result.getStatus());
            assertEquals(BigDecimal.valueOf(100), result.getAmount());

            verify(eventPublisher).publishEvent(any(com.example.carrental.event.PaymentReceivedEvent.class));
        }

        @Test
        @DisplayName("Should throw exception if gateway says payment is not successful")
        void shouldThrowIfGatewaySaysNotPaid() {
            Payment payment = new Payment();
            payment.setId(55L);
            payment.setStatus(PaymentStatus.PENDING);
            String sessionId = "sess_fail";

            when(paymentRepository.findBySessionId(sessionId)).thenReturn(Optional.of(payment));
            when(paymentGateway.isPaymentSuccessful(sessionId)).thenReturn(false);

            assertThrows(RuntimeException.class, () -> paymentService.handlePaymentSuccess(sessionId));
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should mark payment as CANCELED")
        void shouldMarkPaymentAsCancelled() {
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
}