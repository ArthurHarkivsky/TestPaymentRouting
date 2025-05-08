package com.katanapay.routing.service;

import com.katanapay.routing.dto.PaymentRequest;
import com.katanapay.routing.dto.ProviderPaymentResponse;
import com.katanapay.routing.model.entity.Payment;

/**
 * Service interface for provider routing operations.
 * Responsible for determining the appropriate payment provider
 * and routing payment requests to it.
 */
public interface ProviderRoutingService {

    /**
     * Determines the optimal payment provider based on payment details.
     *
     * @param paymentRequest the payment request containing card details, amount, currency, etc.
     * @return the identifier of the selected provider
     */
    String determineProvider(PaymentRequest paymentRequest);

    /**
     * Routes a payment to the previously determined provider.
     *
     * @param payment the payment entity with provider already determined
     * @return the response from the payment provider
     */
    ProviderPaymentResponse routePayment(Payment payment);
}