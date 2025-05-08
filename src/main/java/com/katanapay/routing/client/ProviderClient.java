package com.katanapay.routing.client;

import com.katanapay.routing.model.dto.ProviderPaymentRequest;
import com.katanapay.routing.model.dto.ProviderPaymentResponse;

/**
 * Interface for external payment provider API clients.
 */
public interface ProviderClient {

    /**
     * Sends a payment request to an external payment provider API.
     *
     * @param endpoint the API endpoint URL
     * @param request  the payment request to send
     * @return the provider's response
     */
    ProviderPaymentResponse sendPaymentRequest(String endpoint, ProviderPaymentRequest request);
}