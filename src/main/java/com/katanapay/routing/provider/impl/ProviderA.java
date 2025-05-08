package com.katanapay.routing.provider.impl;

import com.katanapay.routing.client.ProviderClient;
import com.katanapay.routing.exception.ProviderException;
import com.katanapay.routing.model.dto.ProviderPaymentRequest;
import com.katanapay.routing.model.dto.ProviderPaymentResponse;
import com.katanapay.routing.provider.PaymentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Implementation of Provider A payment processor.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderA implements PaymentProvider {

    private static final String PROVIDER_NAME = "PROVIDER_A";

    private final ProviderClient providerClient;

    @Value("${provider.endpoints.provider-a}")
    private String providerEndpoint;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Provider A has specific requirements:
     * - Expects masked card numbers for PCI compliance
     * - Requires specific handling for certain currencies
     */
    @Override
    public ProviderPaymentResponse processPayment(ProviderPaymentRequest request) {
        log.info("Processing payment with Provider A: {}", request.getPaymentId());

        try {
            ProviderPaymentRequest modifiedRequest = preprocessRequest(request);

            ProviderPaymentResponse response = providerClient.sendPaymentRequest(
                    providerEndpoint,
                    modifiedRequest
            );

            log.info("Provider A successfully processed payment: {}", request.getPaymentId());

            return response;
        } catch (Exception e) {
            log.error("Provider A failed to process payment: {}", request.getPaymentId(), e);
            throw new ProviderException("Provider A payment processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Provider-specific preprocessing for the request.
     *
     * @param request original request
     * @return modified request according to Provider A requirements
     */
    private ProviderPaymentRequest preprocessRequest(ProviderPaymentRequest request) {
        // Create a copy of the request to avoid modifying the original
        ProviderPaymentRequest modifiedRequest = ProviderPaymentRequest.builder()
                .paymentId(request.getPaymentId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .bin(request.getBin())
                .build();

        String maskedCardNumber = request.getCardNumber();
        if (maskedCardNumber != null && maskedCardNumber.length() >= 4) {
            maskedCardNumber = "*".repeat(12) + maskedCardNumber.substring(maskedCardNumber.length() - 4);
        }
        modifiedRequest.setCardNumber(maskedCardNumber);

        return modifiedRequest;
    }
}