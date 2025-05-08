package com.katanapay.routing.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.katanapay.routing.dto.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
public class PaymentPerformanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private PaymentRequest samplePaymentRequest;

    @BeforeEach
    void setUp() {
        samplePaymentRequest = PaymentRequest.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .cardNumber("4111111111111111")
                .build();

        WireMock.stubFor(WireMock.post(WireMock.urlPathMatching("/api/v1/providerA/payments"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"paymentId\":\"PERF-A-000\",\"status\":\"COMPLETED\",\"providerReference\":\"PERF-PROV-A-123\"}")));

        WireMock.stubFor(WireMock.post(WireMock.urlPathMatching("/api/v1/providerB/payments"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"paymentId\":\"PERF-B-001\",\"status\":\"COMPLETED\",\"providerReference\":\"PERF-PROV-B-456\"}")));
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldHandle1RequestPerSecond() throws Exception {
        int requestsPerSecond = 1;
        int testDurationSeconds = 10;
        PerformanceTestResult result = executeLoadTest(requestsPerSecond, testDurationSeconds);
        assertThat(result.totalRequests()).isEqualTo(testDurationSeconds * requestsPerSecond);
        assertThat(result.successRate()).isGreaterThanOrEqualTo(0.95);
        assertThat(result.averageLatencyMs()).isLessThan(500);
        System.out.printf("1 req/s test - Throughput: %.2f req/s, Avg latency: %.2f ms, Success rate: %.2f%%%n",
                result.actualThroughput(), result.averageLatencyMs(), result.successRate() * 100);
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldHandle10RequestsPerSecond() throws Exception {
        int requestsPerSecond = 10;
        int testDurationSeconds = 10;
        PerformanceTestResult result = executeLoadTest(requestsPerSecond, testDurationSeconds);
        assertThat(result.totalRequests()).isEqualTo(testDurationSeconds * requestsPerSecond);
        assertThat(result.successRate()).isGreaterThanOrEqualTo(0.95);
        assertThat(result.averageLatencyMs()).isLessThan(1000);
        System.out.printf("10 req/s test - Throughput: %.2f req/s, Avg latency: %.2f ms, Success rate: %.2f%%%n",
                result.actualThroughput(), result.averageLatencyMs(), result.successRate() * 100);
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void shouldHandle100RequestsPerSecond() throws Exception {
        int requestsPerSecond = 100;
        int testDurationSeconds = 10;
        PerformanceTestResult result = executeLoadTest(requestsPerSecond, testDurationSeconds);
        assertThat(result.successRate()).isGreaterThanOrEqualTo(0.90);
        System.out.printf("100 req/s test - Throughput: %.2f req/s, Avg latency: %.2f ms, Success rate: %.2f%%%n",
                result.actualThroughput(), result.averageLatencyMs(), result.successRate() * 100);
    }

    private PerformanceTestResult executeLoadTest(int requestsPerSecond, int durationSeconds) throws Exception {
        int totalRequests = requestsPerSecond * durationSeconds;
        long periodBetweenRequestsMs = (requestsPerSecond > 0) ? (1000 / requestsPerSecond) : 0;
        List<CompletableFuture<RequestResult>> futures = new ArrayList<>();
        int threadPoolSize = Math.min(Math.max(10, requestsPerSecond * 2), 200);

        try (ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
             ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)) {

            Instant testStartTime = Instant.now();
            for (int taskIndex = 0; taskIndex < totalRequests; taskIndex++) {
                CompletableFuture<RequestResult> future = new CompletableFuture<>();
                futures.add(future);
                scheduler.schedule(() -> {
                    Instant requestStartTime = Instant.now();
                    try {
                        CompletableFuture.supplyAsync(() -> {
                            try {
                                return mockMvc.perform(post("/api/v1/payments")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(samplePaymentRequest)))
                                        .andReturn();
                            } catch (Exception e) {
                                throw new CompletionException(e);
                            }
                        }, executor).thenAccept(mvcResult -> {
                            long latencyMs = Duration.between(requestStartTime, Instant.now()).toMillis();
                            boolean success = mvcResult.getResponse().getStatus() == HttpStatus.CREATED.value();
                            future.complete(new RequestResult(success, latencyMs));
                        }).exceptionally(ex -> {
                            long latencyMs = Duration.between(requestStartTime, Instant.now()).toMillis();
                            future.complete(new RequestResult(false, latencyMs));
                            return null;
                        });
                    } catch (Exception e) {
                        future.complete(new RequestResult(false, 0));
                    }
                }, (long) taskIndex * periodBetweenRequestsMs, TimeUnit.MILLISECONDS);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).exceptionally(ex -> null).get(durationSeconds + 10, TimeUnit.SECONDS);

            Instant testEndTime = Instant.now();
            double actualDurationSeconds = Duration.between(testStartTime, testEndTime).toMillis() / 1000.0;

            List<RequestResult> results = futures.stream().map(f -> {
                try {
                    return f.isDone() ? f.getNow(new RequestResult(false, 0)) : new RequestResult(false, 0);
                } catch (Exception e) {
                    return new RequestResult(false, 0);
                }
            }).toList();

            int successfulRequests = (int) results.stream().filter(RequestResult::success).count();
            double successRate = totalRequests > 0 ? (double) successfulRequests / totalRequests : 0;
            double averageLatencyMs = results.stream().filter(RequestResult::success).mapToLong(RequestResult::latencyMs).average().orElse(0);
            double actualThroughput = actualDurationSeconds > 0 ? successfulRequests / actualDurationSeconds : 0;

            return new PerformanceTestResult(totalRequests, successfulRequests, successRate, averageLatencyMs, actualThroughput);

        }
    }

    private record RequestResult(boolean success, long latencyMs) {
    }

    private record PerformanceTestResult(int totalRequests, int successfulRequests, double successRate, double averageLatencyMs,
                                         double actualThroughput) {
    }
}