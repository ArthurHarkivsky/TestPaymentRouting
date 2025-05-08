package com.katanapay.routing.service;

import com.katanapay.routing.dto.PaymentRequest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ProviderRoutingServiceTest {

    @InjectMocks
    private MainProviderRoutingService providerRoutingService;

    private static Stream<Arguments> providePaymentsForRouting() {
        return Stream.of(
                // Test case 1: Visa card (starts with 4) should route to Provider A
                Arguments.of(
                        PaymentRequest.builder()
                                .amount(new BigDecimal("100.00"))
                                .currency("USD")
                                .cardNumber("4111111111111111")
                                .build(),
                        "PROVIDER_A"
                ),

                // Test case 2: Mastercard (starts with 5) should route to Provider B
                Arguments.of(
                        PaymentRequest.builder()
                                .amount(new BigDecimal("200.00"))
                                .currency("EUR")
                                .cardNumber("5555555555554444")
                                .build(),
                        "PROVIDER_B"
                ),

                // Test case 3: High amount payment should route to Provider B regardless of card type
                Arguments.of(
                        PaymentRequest.builder()
                                .amount(new BigDecimal("10000.00"))
                                .currency("USD")
                                .cardNumber("4111111111111111")
                                .build(),
                        "PROVIDER_A"
                ),

                // Test case 4: EUR currency with Visa card should route to Provider A
                Arguments.of(
                        PaymentRequest.builder()
                                .amount(new BigDecimal("50.00"))
                                .currency("EUR")
                                .cardNumber("4111111111111111")
                                .build(),
                        "PROVIDER_A"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("providePaymentsForRouting")
    void shouldRoutePaymentToCorrectProvider(PaymentRequest payment, String expectedProvider) {
        // When
        String selectedProvider = providerRoutingService.determineProvider(payment);

        // Then
        assertThat(selectedProvider).isEqualTo(expectedProvider);
    }
}