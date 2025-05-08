package com.katanapay.routing.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.katanapay.routing.model.dto.PaymentRequest;
import com.katanapay.routing.model.dto.PaymentResponse;
import com.katanapay.routing.model.entity.Payment;
import com.katanapay.routing.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0)
public class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        stubFor(WireMock.post(urlPathMatching("/api/v1/providerA/payments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"paymentId\":\"ABC000\",\"status\":\"COMPLETED\",\"providerReference\":\"PROV-A-123\"}")));

        stubFor(WireMock.post(urlPathMatching("/api/v1/providerB/payments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"paymentId\":\"ABC001\",\"status\":\"COMPLETED\",\"providerReference\":\"PROV-B-456\"}")));
    }

    @Test
    void whenInitiatePayment_thenCreatePaymentAndRouteToProvider() throws Exception {
        // Given
        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .cardNumber("4111111111111111")
                .build();

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn();

        // Then
        PaymentResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                PaymentResponse.class);

        // Verify payment is stored in the database
        Optional<Payment> savedPayment = paymentRepository.findById(response.getId());
        assertThat(savedPayment).isPresent();
        assertThat(savedPayment.get().getStatus()).isEqualTo(Payment.Status.COMPLETED);
        assertThat(savedPayment.get().getProvider()).isNotEmpty();
        assertThat(savedPayment.get().getProviderReference()).isNotEmpty();
    }

    @Test
    void whenGetPayment_thenReturnPaymentDetails() throws Exception {
        // Given
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .amount(new BigDecimal("200.00"))
                .currency("EUR")
                .cardNumber("5555555555554444")
                .bin("555555")
                .provider("PROVIDER_B")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status(Payment.Status.COMPLETED)
                .providerReference("TEST-REF-789")
                .build();

        paymentRepository.save(payment);

        // When/Then
        mockMvc.perform(get("/api/v1/payments/{id}", payment.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.getId().toString()))
                .andExpect(jsonPath("$.amount").value("200.0"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.providerReference").value("TEST-REF-789"));
    }

    @Test
    void whenProviderFails_thenRetryAndRecoverGracefully() throws Exception {
        // Given - Configure provider to fail first, then succeed
        stubFor(WireMock.post(urlPathMatching("/api/v1/providerA/payments"))
                .inScenario("retry-scenario")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("Failed-Once"));

        stubFor(WireMock.post(urlPathMatching("/api/v1/providerB/payments"))
                .inScenario("retry-scenario")
                .whenScenarioStateIs("Failed-Once")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"paymentId\":\"ABC002\",\"status\":\"COMPLETED\",\"providerReference\":\"RETRY-SUCCESS\"}")));

        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .cardNumber("4111111111111111")
                .build();

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn();

        // Then
        PaymentResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                PaymentResponse.class);

        Optional<Payment> savedPayment = paymentRepository.findById(response.getId());
        assertThat(savedPayment).isPresent();
        assertThat(savedPayment.get().getProviderReference()).isEqualTo("PROV-A-123");
    }
}