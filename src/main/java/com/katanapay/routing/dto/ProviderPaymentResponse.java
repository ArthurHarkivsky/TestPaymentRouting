package com.katanapay.routing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderPaymentResponse {

    private String paymentId;
    private String providerReference;
    private String status;
}