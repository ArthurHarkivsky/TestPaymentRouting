package com.katanapay.routing.provider;

import com.katanapay.routing.client.ProviderClient;
import com.katanapay.routing.exception.ProviderException;
import com.katanapay.routing.dto.ProviderPaymentRequest;
import com.katanapay.routing.dto.ProviderPaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Implementation of the Provider B payment processor.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderB implements PaymentProvider {

    private static final String PROVIDER_NAME = "PROVIDER_B";

    private final ProviderClient providerClient;

    @Value("${provider.endpoints.provider-b}")
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
     * Provider B has specific requirements:
     * - Requires amounts to be in cents (multiply by 100)
     * - Has different response format handling
     */
    @Override
    public ProviderPaymentResponse processPayment(ProviderPaymentRequest request) {
        log.info("Processing payment with Provider B: {}", request.getPaymentId());

        try {
            ProviderPaymentRequest modifiedRequest = preprocessRequest(request);

            ProviderPaymentResponse response = providerClient.sendPaymentRequest(
                    providerEndpoint,
                    modifiedRequest
            );

            log.info("Provider B successfully processed payment: {}", request.getPaymentId());

            return response;
        } catch (Exception e) {
            log.error("Provider B failed to process payment: {}", request.getPaymentId(), e);
            throw new ProviderException("Provider B payment processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Provider-specific preprocessing for the request.
     *
     * @param request original request
     * @return modified request according to Provider B requirements
     */
    private ProviderPaymentRequest preprocessRequest(ProviderPaymentRequest request) {
        // Create a copy of the request to avoid modifying the original
        ProviderPaymentRequest modifiedRequest = ProviderPaymentRequest.builder()
                .paymentId(request.getPaymentId())
                .bin(request.getBin())
                .cardNumber(request.getCardNumber())
                .currency(request.getCurrency())
                .build();

        // Provider B expects the amount in cents (multiply by 100)
        if (request.getAmount() != null) {
            BigDecimal amountInCents = request.getAmount()
                    .multiply(new BigDecimal("100"))
                    .setScale(0, RoundingMode.HALF_UP);
            modifiedRequest.setAmount(amountInCents);
        }

        return modifiedRequest;
    }
}