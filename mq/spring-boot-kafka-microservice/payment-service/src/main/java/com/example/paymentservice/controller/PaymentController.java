package com.example.paymentservice.controller;

import com.example.paymentservice.model.PaymentConfirmation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Controller for testing the payment confirmation to shipping notification flow.
 * This is used in the scenario: Payment Confirmation → Kafka → Shipping Notification
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final KafkaTemplate<String, PaymentConfirmation> paymentConfirmationKafkaTemplate;

    /**
     * Endpoint to simulate a payment confirmation and send it to Kafka.
     * This is for testing purposes only.
     * 
     * @param orderId The order ID to create a payment confirmation for
     * @return The created payment confirmation
     */
    @PostMapping("/confirm/{orderId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<PaymentConfirmation> confirmPayment(@PathVariable String orderId) {
        log.info("Simulating payment confirmation for order: {}", orderId);
        
        // Create a payment confirmation
        PaymentConfirmation paymentConfirmation = PaymentConfirmation.builder()
                .orderId(orderId)
                .customerId("customer-" + UUID.randomUUID().toString().substring(0, 8))
                .amount(BigDecimal.valueOf(Math.random() * 1000).setScale(2, BigDecimal.ROUND_HALF_UP))
                .paymentDate(LocalDateTime.now())
                .paymentStatus("COMPLETED")
                .paymentMethod("CREDIT_CARD")
                .transactionId(UUID.randomUUID().toString())
                .build();
        
        // Send payment confirmation to Kafka
        log.info("Sending payment confirmation to Kafka: {}", paymentConfirmation);
        paymentConfirmationKafkaTemplate.send("payment-confirmation-topic", orderId, paymentConfirmation);
        
        return Mono.just(paymentConfirmation);
    }
}