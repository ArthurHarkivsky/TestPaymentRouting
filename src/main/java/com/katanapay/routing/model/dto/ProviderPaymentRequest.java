package com.katanapay.routing.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderPaymentRequest {

    private UUID paymentId;

    private BigDecimal amount;
    private String currency;

    private String cardNumber;
    private String bin;
}