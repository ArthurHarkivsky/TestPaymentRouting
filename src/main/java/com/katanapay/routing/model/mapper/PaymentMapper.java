package com.katanapay.routing.model.mapper;

import com.katanapay.routing.model.dto.PaymentRequest;
import com.katanapay.routing.model.dto.PaymentResponse;
import com.katanapay.routing.model.dto.ProviderPaymentRequest;
import com.katanapay.routing.model.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "provider", ignore = true)
    @Mapping(target = "status", constant = "INITIATED")
    @Mapping(target = "providerReference", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    Payment toEntity(PaymentRequest request);

    @Mapping(source = "cardNumber", target = "maskedCardNumber", qualifiedByName = "maskCardNumber")
    PaymentResponse toResponse(Payment payment);

    @Mapping(source = "id", target = "paymentId")
    ProviderPaymentRequest toProviderRequest(Payment payment);

    @Named("maskCardNumber")
    default String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 16) {
            return cardNumber;
        }
        return "*".repeat(12) + cardNumber.substring(12);
    }
}