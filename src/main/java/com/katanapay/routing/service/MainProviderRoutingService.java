package com.katanapay.routing.service;

import com.katanapay.routing.exception.RoutingException;
import com.katanapay.routing.dto.PaymentRequest;
import com.katanapay.routing.dto.ProviderPaymentResponse;
import com.katanapay.routing.model.entity.Payment;
import com.katanapay.routing.model.mapper.PaymentMapper;
import com.katanapay.routing.provider.PaymentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.CircuitBreaker;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of a provider routing service.
 * Contains the logic for selecting payment providers based on various criteria
 * and handles routing payments to the selected provider.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MainProviderRoutingService implements ProviderRoutingService {

    private final PaymentMapper paymentMapper;
    private final List<PaymentProvider> providers;
    private Map<String, PaymentProvider> providersByName;

    /**
     * {@inheritDoc}
     * <p>
     * Provider determination logic:
     * - BIN range 400000-499999: Use Provider A
     * - BIN range 500000-599999: Use Provider B
     * - For USD currency over $1000: Use Provider B
     * - For other currencies over equivalent of $1000: Use Provider A
     * - Default: Use Provider A
     */
    @Override
    public String determineProvider(PaymentRequest paymentRequest) {
        log.debug("Determining provider for payment request: {}", paymentRequest);

        String bin = paymentRequest.getBin();
        String currency = paymentRequest.getCurrency();
        BigDecimal amount = paymentRequest.getAmount();

        // BIN-based routing
        if (bin != null) {
            int binPrefix = Integer.parseInt(bin.substring(0, 1));

            // Visa BIN range (4xxxxx)
            if (binPrefix == 4) {
                log.debug("Routing to Provider A based on Visa BIN range");
                return "PROVIDER_A";
            }

            // Mastercard BIN range (5xxxxx)
            if (binPrefix == 5) {
                log.debug("Routing to Provider B based on Mastercard BIN range");
                return "PROVIDER_B";
            }
        }

        // Amount-based routing
        BigDecimal threshold = new BigDecimal("1000.00");

        if (amount.compareTo(threshold) > 0) {
            if ("USD".equals(currency)) {
                log.debug("Routing to Provider B based on high USD amount");
                return "PROVIDER_B";
            } else {
                log.debug("Routing to Provider A based on high non-USD amount");
                return "PROVIDER_A";
            }
        }

        // Default to Provider A
        log.debug("Using default routing to Provider A");
        return "PROVIDER_A";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @CircuitBreaker
    @Retryable(
            retryFor = {Exception.class},
            backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public ProviderPaymentResponse routePayment(Payment payment) {
        log.info("Routing payment to provider: {}", payment.getProvider());

        if (providersByName == null) {
            providersByName = providers.stream()
                    .collect(Collectors.toMap(PaymentProvider::getProviderName, Function.identity()));
        }

        PaymentProvider provider = providersByName.get(payment.getProvider());

        if (provider == null) {
            throw new RoutingException("Unknown provider: " + payment.getProvider());
        }

        return provider.processPayment(paymentMapper.toProviderRequest(payment));
    }
}