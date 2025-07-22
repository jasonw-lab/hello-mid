package com.example.notificationservice.listener;

import com.example.notificationservice.model.Order;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "order-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCreated(Order order) {
        log.info("Received order: {}", order);
        
        try {
            // Send notification for the order
            notificationService.sendNotification(order)
                .doOnSuccess(v -> log.info("Notification sent for order: {}", order.getOrderId()))
                .doOnError(e -> log.error("Error sending notification for order: {}", order.getOrderId(), e))
                .subscribe();
        } catch (Exception e) {
            log.error("Error handling order message: {}", order, e);
        }
    }
}