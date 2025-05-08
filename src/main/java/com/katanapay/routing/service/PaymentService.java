package com.katanapay.routing.service;

import com.katanapay.routing.dto.PaymentRequest;
import com.katanapay.routing.dto.PaymentResponse;

import java.util.UUID;

/**
 * Service interface for payment processing operations.
 */
public interface PaymentService {

    /**
     * Process a payment request by determining the optimal provider and submitting to it.
     *
     * @param paymentRequest the payment request data
     * @return a response containing payment details and status
     */
    PaymentResponse processPayment(PaymentRequest paymentRequest);

    /**
     * Retrieve payment details by payment ID.
     *
     * @param id the payment ID
     * @return the payment details
     * @throws com.katanapay.routing.exception.RoutingException if payment not found
     */
    PaymentResponse getPayment(UUID id);

    /**
     * Update payment status after provider processing.
     *
     * @param paymentId         the payment ID
     * @param providerReference the reference ID from the provider
     * @param status            the new status
     * @return the updated payment details
     */
    // todo use in API
    @SuppressWarnings("unused")
    PaymentResponse updatePaymentStatus(UUID paymentId, String providerReference, String status);
}