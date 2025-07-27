package com.example.paymentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model class representing a payment confirmation message.
 * This is used in the scenario: Payment Confirmation → Kafka → Shipping Notification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmation {
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private String paymentStatus;
    private String paymentMethod;
    private String transactionId;
}