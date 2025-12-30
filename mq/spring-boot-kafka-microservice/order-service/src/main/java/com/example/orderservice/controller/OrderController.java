package com.example.orderservice.controller;

import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderConfirmed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final KafkaTemplate<String, Order> kafkaTemplate;
    private final KafkaTemplate<String, String> eventKafkaTemplate;
    private final ObjectMapper objectMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Order> createOrder(@RequestBody Order order) {
        log.info("Received order request: {}", order);

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

    /**
     * Simulator endpoint for sending OrderConfirmed events to Kafka
     * Used for testing alert-streams-service Rule A/B/C detection
     */
    @PostMapping("/sim/order/confirmed")
    public ResponseEntity<String> simulateOrderConfirmed(@RequestBody OrderConfirmedRequest request) {
        log.info("Received OrderConfirmed simulation request: orderId={}, occurredAt={}",
                request.getOrderId(), request.getOccurredAt());

        try {
            String orderId = request.getOrderId();
            String occurredAt = request.getOccurredAt() != null ?
                request.getOccurredAt() :
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            OrderConfirmed event = OrderConfirmed.builder()
                .eventType("OrderConfirmed")
                .eventId(UUID.randomUUID().toString())
                .occurredAt(occurredAt)
                .orderId(orderId)
                .build();

            String jsonEvent = objectMapper.writeValueAsString(event);
            log.info("Sending OrderConfirmed event to Kafka: {}", jsonEvent);

            eventKafkaTemplate.send("orders.events.v1", orderId, jsonEvent);

            return ResponseEntity.ok("OrderConfirmed event sent successfully");
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OrderConfirmed event", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to process OrderConfirmed event");
        }
    }

    /**
     * Request model for OrderConfirmed simulation
     */
    public static class OrderConfirmedRequest {
        private String orderId;
        private String occurredAt;

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getOccurredAt() { return occurredAt; }
        public void setOccurredAt(String occurredAt) { this.occurredAt = occurredAt; }
    }
}