package com.example.carrental.controller;

import com.example.carrental.dto.payment.CreateFineDto;
import com.example.carrental.dto.payment.PaymentRequestDto;
import com.example.carrental.dto.payment.PaymentResponseDto;
import com.example.carrental.enums.payment.PaymentStatus;
import com.example.carrental.enums.payment.PaymentType;
import com.example.carrental.service.impl.StripeWebhookService;
import com.example.carrental.service.interfaces.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.telegram.telegrambots.meta.TelegramBotsApi;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "bot.token=test-token",
        "bot.username=test-bot",
        "bot.admin-chat-id=123"
})
@AutoConfigureMockMvc
public class PaymentControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TelegramBotsApi telegramBotsApi;

    @MockitoBean
    private StripeWebhookService stripeWebhookService;

    @MockitoBean
    private PaymentService paymentService;

    @Nested
    @DisplayName("Create Session Tests")
    class CreateSessionTests {

        @Test
        @DisplayName("Should return 201 and URL when authorized")
        @WithMockUser
        @SneakyThrows
        void shouldReturn201AndUrlWhenAuthorized() {
            PaymentRequestDto requestDto = new PaymentRequestDto();
            requestDto.setRentalId(1L);
            requestDto.setType(PaymentType.PAYMENT);

            PaymentResponseDto mockResponse = new PaymentResponseDto();
            mockResponse.setSessionUrl("https://checkout.stripe.com/test-url");
            mockResponse.setStatus(PaymentStatus.PENDING);
            mockResponse.setAmount(BigDecimal.valueOf(100));

            when(paymentService.createPaymentSession(any(PaymentRequestDto.class)))
                    .thenReturn(mockResponse);

            mockMvc.perform(post("/api/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sessionUrl")
                            .value("https://checkout.stripe.com/test-url"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("Should fail with 401 if not authorized")
        @SneakyThrows
        void shouldFailWith401IfNotAuthorized() {
            PaymentRequestDto requestDto = new PaymentRequestDto();
            requestDto.setRentalId(1L);
            requestDto.setType(PaymentType.PAYMENT);

            mockMvc.perform(post("/api/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Issue Fine Tests (Manager Only)")
    class IssueFineTests {

        @Test
        @DisplayName("Success: Manager can issue a fine")
        @WithMockUser(username = "manager", roles = "MANAGER")
        @SneakyThrows
        void shouldCreateFineWhenManager() {
            Long rentalId = 1L;
            CreateFineDto fineDto = new CreateFineDto(BigDecimal.valueOf(50.00), "DAMAGE");

            PaymentResponseDto mockResponse = new PaymentResponseDto();
            mockResponse.setStatus(PaymentStatus.PENDING);
            mockResponse.setType(PaymentType.FINE);
            mockResponse.setAmount(BigDecimal.valueOf(50.00));

            when(paymentService.createFine(eq(rentalId), any(CreateFineDto.class)))
                    .thenReturn(mockResponse);

            mockMvc.perform(post("/api/payments/rentals/{id}/fine", rentalId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(fineDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("FINE"))
                    .andExpect(jsonPath("$.amount").value(50.0));
        }

        @Test
        @DisplayName("Fail: Customer cannot issue a fine (403 Forbidden)")
        @WithMockUser(username = "customer", roles = "CUSTOMER")
        @SneakyThrows
        void shouldForbidFineCreationWhenCustomer() {
            Long rentalId = 1L;
            CreateFineDto fineDto = new CreateFineDto(BigDecimal.valueOf(50.00), "DAMAGE");

            mockMvc.perform(post("/api/payments/rentals/{id}/fine", rentalId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(fineDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Fail: Should return 400 if amount is invalid")
        @WithMockUser(username = "manager", roles = "MANAGER")
        @SneakyThrows
        void shouldReturn400WhenAmountInvalid() {
            CreateFineDto fineDto = new CreateFineDto(BigDecimal.valueOf(-10.00), "DAMAGE");

            mockMvc.perform(post("/api/payments/rentals/{id}/fine", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(fineDto)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Callback Tests (Success/Cancel/Webhook)")
    class CallbackTests {

        @Test
        @DisplayName("Should work WITHOUT Authentication (permitAll)")
        @SneakyThrows
        void shouldWorkWithoutAuthentication() {
            String sessionId = "cs_test_123";

            PaymentResponseDto mockResponse = new PaymentResponseDto();
            mockResponse.setStatus(PaymentStatus.PAID);
            mockResponse.setSessionId(sessionId);

            when(paymentService.handlePaymentSuccess(sessionId))
                    .thenReturn(mockResponse);

            mockMvc.perform(get("/api/payments/success")
                            .param("session_id", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PAID"));
        }

        @Test
        @DisplayName("Cancel should work WITHOUT Authentication")
        @SneakyThrows
        void cancelShouldWorkWithoutAuthentication() {
            mockMvc.perform(get("/api/payments/cancel")
                            .param("session_id", "sess_123"))
                    .andExpect(status().isOk());

            verify(paymentService).handlePaymentCancel("sess_123");
        }

        @Test
        @DisplayName("Webhook should return 200 OK WITHOUT Authentication")
        @SneakyThrows
        void webhookShouldReturn200() {
            String payload = "{\"type\":\"checkout.session.completed\"}";
            String fakeSignature = "t=12345,v1=fake_signature_hash";

            doNothing().when(stripeWebhookService).processWebhook(payload, fakeSignature);

            mockMvc.perform(post("/api/payments/webhook")
                            .header("Stripe-Signature", fakeSignature)
                            .content(payload)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.content()
                            .string("Webhook processed successfully"));
        }
    }
}
