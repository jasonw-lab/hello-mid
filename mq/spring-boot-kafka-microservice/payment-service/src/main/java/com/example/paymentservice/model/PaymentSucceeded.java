package com.example.paymentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PaymentSucceeded event model for Kafka alert simulation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSucceeded {
    private String eventType;
    private String eventId;
    private String occurredAt;
    private String orderId;
    private String paymentId;
    private String provider;
    private Integer amount;
    private String currency;
}
