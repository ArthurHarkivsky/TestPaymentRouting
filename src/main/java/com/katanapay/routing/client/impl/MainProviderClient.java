package com.katanapay.routing.client.impl;

import com.katanapay.routing.client.ProviderClient;
import com.katanapay.routing.exception.ProviderException;
import com.katanapay.routing.model.dto.ProviderPaymentRequest;
import com.katanapay.routing.model.dto.ProviderPaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.CircuitBreaker;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.UUID;

/**
 * Implementation of the provider client interface.
 * Handles HTTP communication with external payment providers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MainProviderClient implements ProviderClient {

    private final RestTemplate restTemplate;

    /**
     * {@inheritDoc}
     */
    @Override
    @Retryable(
            retryFor = {ResourceAccessException.class},
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @CircuitBreaker
    public ProviderPaymentResponse sendPaymentRequest(String endpoint, ProviderPaymentRequest request) {
        log.debug("Sending payment request to endpoint: {} for payment: {}", endpoint, request.getPaymentId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // For a real implementation, add authentication headers here
        // headers.set("X-API-Key", apiKey);

        HttpEntity<ProviderPaymentRequest> entity = new HttpEntity<>(request, headers);

        try {
            // In a mock environment, this would call a stub/mock endpoint
            ResponseEntity<ProviderPaymentResponse> response = restTemplate.postForEntity(
                    endpoint,
                    entity,
                    ProviderPaymentResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("Received successful response from provider API for payment: {}",
                        request.getPaymentId());
                return response.getBody();
            } else {
                log.error("Provider API returned unsuccessful response for payment: {}",
                        request.getPaymentId());
                throw new ProviderException("Provider API returned unsuccessful response");
            }
        } catch (HttpStatusCodeException e) {
            log.error("Provider API returned error status: {} for payment: {}",
                    e.getStatusCode(), request.getPaymentId());

            // For the purpose of this example, simulate a successful response with COMPLETED status
            // In a real implementation, handle different error codes appropriately
            if (e.getStatusCode().is4xxClientError()) {
                return simulateFallbackResponse(request.getPaymentId(), "FAILED");
            } else {
                throw new ProviderException("Provider API error: " + e.getMessage(), e);
            }
        } catch (ResourceAccessException e) {
            log.error("Connection error to provider API for payment: {}", request.getPaymentId(), e);
            throw new ProviderException("Provider API connection error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error calling provider API for payment: {}", request.getPaymentId(), e);
            throw new ProviderException("Unexpected provider API error: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a simulated response for testing or fallback purposes.
     *
     * @param paymentId the payment ID
     * @param status    the status to return
     * @return a simulated provider response
     */
    private ProviderPaymentResponse simulateFallbackResponse(UUID paymentId, String status) {
        return ProviderPaymentResponse.builder()
                .paymentId(paymentId)
                .status(status)
                .providerReference("SIM_" + System.currentTimeMillis())
                .build();
    }
}