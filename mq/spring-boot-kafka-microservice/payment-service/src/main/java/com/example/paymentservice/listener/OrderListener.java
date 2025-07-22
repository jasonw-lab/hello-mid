package com.example.paymentservice.listener;

import com.example.paymentservice.model.Order;
import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderListener {

    private final PaymentService paymentService;

    @KafkaListener(topics = "order-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCreated(Order order) {
        log.info("Received order: {}", order);
        
        try {
            // Process payment for the order
            paymentService.processPayment(order)
                .doOnSuccess(v -> log.info("Payment processed for order: {}", order.getOrderId()))
                .doOnError(e -> log.error("Error processing payment for order: {}", order.getOrderId(), e))
                .subscribe();
        } catch (Exception e) {
            log.error("Error handling order message: {}", order, e);
        }
    }
}