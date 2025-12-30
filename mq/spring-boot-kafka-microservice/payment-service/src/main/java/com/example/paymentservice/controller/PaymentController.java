package com.example.paymentservice.controller;

import com.example.paymentservice.model.PaymentConfirmation;
import com.example.paymentservice.model.PaymentSucceeded;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final KafkaTemplate<String, String> eventKafkaTemplate;
    private final ObjectMapper objectMapper;

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
        log.info("Received payment confirmation request for orderId: {}", orderId);

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

    /**
     * Simulator endpoint for sending PaymentSucceeded events to Kafka
     * Used for testing alert-streams-service Rule A/B/C detection
     */
    @PostMapping("/sim/payment/succeeded")
    public ResponseEntity<String> simulatePaymentSucceeded(@RequestBody PaymentSucceededRequest request) {
        log.info("Received PaymentSucceeded simulation request: orderId={}, paymentId={}, provider={}, amount={}, currency={}",
                request.getOrderId(), request.getPaymentId(), request.getProvider(),
                request.getAmount(), request.getCurrency());

        try {
            String orderId = request.getOrderId();
            String paymentId = request.getPaymentId() != null ? request.getPaymentId() : "P-" + UUID.randomUUID().toString().substring(0, 8);
            String occurredAt = request.getOccurredAt() != null ?
                request.getOccurredAt() :
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            PaymentSucceeded event = PaymentSucceeded.builder()
                .eventType("PaymentSucceeded")
                .eventId(UUID.randomUUID().toString())
                .occurredAt(occurredAt)
                .orderId(orderId)
                .paymentId(paymentId)
                .provider(request.getProvider() != null ? request.getProvider() : "PayPay")
                .amount(request.getAmount() != null ? request.getAmount() : 1200)
                .currency(request.getCurrency() != null ? request.getCurrency() : "JPY")
                .build();

            String jsonEvent = objectMapper.writeValueAsString(event);
            log.info("Sending PaymentSucceeded event to Kafka: {}", jsonEvent);

            eventKafkaTemplate.send("payments.events.v1", orderId, jsonEvent);

            return ResponseEntity.ok("PaymentSucceeded event sent successfully");
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PaymentSucceeded event", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to process PaymentSucceeded event");
        }
    }

    /**
     * Request model for PaymentSucceeded simulation
     */
    public static class PaymentSucceededRequest {
        private String orderId;
        private String paymentId;
        private String provider;
        private Integer amount;
        private String currency;
        private String occurredAt;

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public Integer getAmount() { return amount; }
        public void setAmount(Integer amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getOccurredAt() { return occurredAt; }
        public void setOccurredAt(String occurredAt) { this.occurredAt = occurredAt; }
    }
}