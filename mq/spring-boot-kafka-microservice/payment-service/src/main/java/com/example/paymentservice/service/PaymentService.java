package com.example.paymentservice.service;

import com.example.paymentservice.model.Order;
import com.example.paymentservice.model.PaymentConfirmation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final KafkaTemplate<String, PaymentConfirmation> paymentConfirmationKafkaTemplate;

    /**
     * Process payment for an order
     * @param order The order to process payment for
     * @return A Mono that completes when the payment is processed
     */
    public Mono<Void> processPayment(Order order) {
        return Mono.fromRunnable(() -> {
            log.info("Processing payment for order: {}", order.getOrderId());
            
            // In a real application, this would call a payment gateway
            // For this example, we'll just simulate payment processing
            
            if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                log.error("Invalid order amount: {}", order.getTotalAmount());
                throw new IllegalArgumentException("Invalid order amount");
            }
            
            // Simulate payment processing delay
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            log.info("Payment processed successfully for order: {}", order.getOrderId());
            
            // Create payment confirmation
            PaymentConfirmation paymentConfirmation = PaymentConfirmation.builder()
                    .orderId(order.getOrderId())
                    .customerId(order.getCustomerId())
                    .amount(order.getTotalAmount())
                    .paymentDate(LocalDateTime.now())
                    .paymentStatus("COMPLETED")
                    .paymentMethod("CREDIT_CARD") // Simulated payment method
                    .transactionId(UUID.randomUUID().toString())
                    .build();
            
            // Send payment confirmation to Kafka
            log.info("Sending payment confirmation to Kafka: {}", paymentConfirmation);
            paymentConfirmationKafkaTemplate.send("payment-confirmation-topic", order.getOrderId(), paymentConfirmation);
        });
    }
}