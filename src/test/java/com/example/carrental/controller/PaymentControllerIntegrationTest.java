package com.example.carrental.controller;

import com.example.carrental.dto.payment.PaymentRequestDto;
import com.example.carrental.dto.payment.PaymentResponseDto;
import com.example.carrental.enums.PaymentStatus;
import com.example.carrental.enums.PaymentType;
import com.example.carrental.service.PaymentService;
import com.example.carrental.util.BaseIntegrationTest;
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

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
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
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
public class PaymentControllerIntegrationTest{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
                    .andExpect(jsonPath("$.sessionUrl").value("https://checkout.stripe.com/test-url"))
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
        void cancelShouldWorkWithAuthentication() {
            mockMvc.perform(get("/api/payments/cancel")
                            .param("session_id", "sess_123"))
                    .andExpect(status().isOk());
        }
    }
}
