package com.katanapay.routing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID id;
    private BigDecimal amount;
    private String currency;
    private String maskedCardNumber;
    private String provider;
    private String status;
    private String providerReference;
    private LocalDateTime createdAt;
}