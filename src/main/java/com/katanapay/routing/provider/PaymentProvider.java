package com.katanapay.routing.provider;

import com.katanapay.routing.dto.ProviderPaymentRequest;
import com.katanapay.routing.dto.ProviderPaymentResponse;

/**
 * Interface for payment providers.
 * Defines the contract that all payment providers must implement.
 */
public interface PaymentProvider {

    /**
     * Returns the provider's unique identifier.
     *
     * @return the provider name
     */
    String getProviderName();

    /**
     * Processes a payment through this provider.
     *
     * @param request the payment request to process
     * @return the provider's response
     */
    ProviderPaymentResponse processPayment(ProviderPaymentRequest request);
}