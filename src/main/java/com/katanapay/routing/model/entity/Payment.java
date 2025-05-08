package com.katanapay.routing.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;
    private BigDecimal amount;
    private String currency;
    private String cardNumber;
    private String bin;
    private String provider;
    @Enumerated(EnumType.STRING)
    private Status status;
    private String providerReference;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Status {
        INITIATED, PROCESSING, COMPLETED, FAILED
    }
}