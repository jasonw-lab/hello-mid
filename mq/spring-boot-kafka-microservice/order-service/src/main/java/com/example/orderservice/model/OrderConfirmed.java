package com.example.orderservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * OrderConfirmed event model for Kafka alert simulation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmed {
    private String eventType;
    private String eventId;
    private String occurredAt;
    private String orderId;
}
