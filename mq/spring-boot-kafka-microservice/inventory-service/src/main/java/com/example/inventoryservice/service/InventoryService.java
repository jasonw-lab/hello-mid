package com.example.inventoryservice.service;

import com.example.inventoryservice.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class InventoryService {

    // In-memory inventory for demonstration purposes
    // In a real application, this would be stored in a database
    private final Map<String, Integer> inventory = new ConcurrentHashMap<>();

    public InventoryService() {
        // Initialize with some sample inventory
        inventory.put("product-1", 100);
        inventory.put("product-2", 200);
        inventory.put("product-3", 300);
    }

    /**
     * Update inventory for an order
     * @param order The order to update inventory for
     * @return A Mono that completes when the inventory is updated
     */
    public Mono<Void> updateInventory(Order order) {
        return Mono.fromRunnable(() -> {
            log.info("Updating inventory for order: {}", order.getOrderId());
            
            if (order.getItems() == null || order.getItems().isEmpty()) {
                log.error("Order has no items: {}", order.getOrderId());
                throw new IllegalArgumentException("Order has no items");
            }
            
            // Check if all products are in stock
            order.getItems().forEach(item -> {
                String productId = item.getProductId();
                int quantity = item.getQuantity();
                
                if (!inventory.containsKey(productId)) {
                    log.error("Product not found in inventory: {}", productId);
                    throw new IllegalArgumentException("Product not found in inventory: " + productId);
                }
                
                int currentStock = inventory.get(productId);
                if (currentStock < quantity) {
                    log.error("Insufficient stock for product: {}. Required: {}, Available: {}", 
                            productId, quantity, currentStock);
                    throw new IllegalArgumentException("Insufficient stock for product: " + productId);
                }
            });
            
            // Update inventory
            order.getItems().forEach(item -> {
                String productId = item.getProductId();
                int quantity = item.getQuantity();
                
                int newStock = inventory.get(productId) - quantity;
                inventory.put(productId, newStock);
                
                log.info("Updated inventory for product: {}. New stock: {}", productId, newStock);
            });
            
            log.info("Inventory updated successfully for order: {}", order.getOrderId());
        });
    }
    
    /**
     * Get current stock for a product
     * @param productId The product ID
     * @return The current stock
     */
    public int getStock(String productId) {
        if (!inventory.containsKey(productId)) {
            throw new IllegalArgumentException("Product not found in inventory: " + productId);
        }
        return inventory.get(productId);
    }
}