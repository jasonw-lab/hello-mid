package com.example.notificationservice.service;

import com.example.notificationservice.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class NotificationService {

    /**
     * Send notification for an order
     * @param order The order to send notification for
     * @return A Mono that completes when the notification is sent
     */
    public Mono<Void> sendNotification(Order order) {
        return Mono.fromRunnable(() -> {
            log.info("Sending notification for order: {}", order.getOrderId());
            
            // In a real application, this would send an email, SMS, or push notification
            // For this example, we'll just log the notification
            
            if (order.getCustomerId() == null || order.getCustomerId().isEmpty()) {
                log.error("Order has no customer ID: {}", order.getOrderId());
                throw new IllegalArgumentException("Order has no customer ID");
            }
            
            // Simulate notification sending delay
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Log different types of notifications
            log.info("Email notification sent to customer {} for order {}", 
                    order.getCustomerId(), order.getOrderId());
            
            log.info("SMS notification sent to customer {} for order {}", 
                    order.getCustomerId(), order.getOrderId());
            
            if (order.getTotalAmount() != null && order.getTotalAmount().doubleValue() > 100) {
                log.info("VIP customer notification sent to sales team for high-value order {}", 
                        order.getOrderId());
            }
            
            log.info("Notification sent successfully for order: {}", order.getOrderId());
        });
    }
}