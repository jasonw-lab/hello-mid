package com.example.orderservice.controller;

import com.example.orderservice.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final KafkaTemplate<String, Order> kafkaTemplate;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Order> createOrder(@RequestBody Order order) {
        // Set order ID and date if not provided
        if (order.getOrderId() == null) {
            order.setOrderId(UUID.randomUUID().toString());
        }
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDateTime.now());
        }
        
        // Set initial status
        order.setStatus("CREATED");
        
        log.info("Sending order to Kafka: {}", order);
        
        // Send the order to Kafka
        kafkaTemplate.send("order-topic", order.getOrderId(), order);
        
        return Mono.just(order);
    }
    
    @GetMapping("/{orderId}")
    public Mono<Order> getOrder(@PathVariable String orderId) {
        // In a real application, this would retrieve the order from a database
        // For this example, we'll just return a dummy order
        return Mono.just(Order.builder()
                .orderId(orderId)
                .customerId("customer-123")
                .status("PROCESSING")
                .orderDate(LocalDateTime.now())
                .build());
    }
}