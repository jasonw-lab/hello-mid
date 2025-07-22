package com.example.paymentservice.service;

import com.example.paymentservice.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@Slf4j
public class PaymentService {

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
        });
    }
}