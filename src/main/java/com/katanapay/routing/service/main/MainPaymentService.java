package com.katanapay.routing.service.main;

import com.katanapay.routing.exception.RoutingException;
import com.katanapay.routing.model.dto.PaymentRequest;
import com.katanapay.routing.model.dto.PaymentResponse;
import com.katanapay.routing.model.dto.ProviderPaymentResponse;
import com.katanapay.routing.model.entity.Payment;
import com.katanapay.routing.model.mapper.PaymentMapper;
import com.katanapay.routing.outbox.OutboxService;
import com.katanapay.routing.repository.PaymentRepository;
import com.katanapay.routing.service.PaymentService;
import com.katanapay.routing.service.ProviderRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MainPaymentService implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OutboxService outboxService;
    private final ProviderRoutingService providerRoutingService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest paymentRequest) {
        log.info("Processing payment request: {}", paymentRequest);

        Payment payment = paymentMapper.toEntity(paymentRequest);
        payment.setId(UUID.randomUUID());

        String provider = providerRoutingService.determineProvider(paymentRequest);
        payment.setProvider(provider);

        payment = paymentRepository.save(payment);

        ProviderPaymentResponse providerResponse = providerRoutingService.routePayment(payment);

        payment.setStatus(Payment.Status.valueOf(providerResponse.getStatus()));
        payment.setProviderReference(providerResponse.getProviderReference());
        payment.setUpdatedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        outboxService.createOutboxEvent("PAYMENT", payment.getId().toString(),
                "PAYMENT_PROCESSED", payment.toString());

        log.info("Payment processed successfully with ID: {}", payment.getId());

        return paymentMapper.toResponse(payment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID id) {
        log.debug("Retrieving payment details for ID: {}", id);

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RoutingException("Payment not found with ID: " + id));

        return paymentMapper.toResponse(payment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PaymentResponse updatePaymentStatus(UUID paymentId, String providerReference, String status) {
        log.debug("Updating payment status: {} for ID: {} with reference: {}", status, paymentId, providerReference);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RoutingException("Payment not found with ID: " + paymentId));

        payment.setStatus(Payment.Status.valueOf(status));
        payment.setProviderReference(providerReference);
        payment.setUpdatedAt(LocalDateTime.now());

        payment = paymentRepository.save(payment);

        outboxService.createOutboxEvent("PAYMENT", payment.getId().toString(),
                "PAYMENT_STATUS_CHANGED", payment.toString());

        return paymentMapper.toResponse(payment);
    }
}