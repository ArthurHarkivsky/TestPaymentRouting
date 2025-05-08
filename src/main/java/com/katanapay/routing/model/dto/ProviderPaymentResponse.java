package com.katanapay.routing.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderPaymentResponse {

    private UUID paymentId;
    private String providerReference;
    private String status;
}