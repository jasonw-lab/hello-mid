package com.example.inventoryservice.listener;

import com.example.inventoryservice.model.Order;
import com.example.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderListener {

    private final InventoryService inventoryService;

    @KafkaListener(topics = "order-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCreated(Order order) {
        log.info("Received order: {}", order);
        
        try {
            // Update inventory for the order
            inventoryService.updateInventory(order)
                .doOnSuccess(v -> log.info("Inventory updated for order: {}", order.getOrderId()))
                .doOnError(e -> log.error("Error updating inventory for order: {}", order.getOrderId(), e))
                .subscribe();
        } catch (Exception e) {
            log.error("Error handling order message: {}", order, e);
        }
    }
}