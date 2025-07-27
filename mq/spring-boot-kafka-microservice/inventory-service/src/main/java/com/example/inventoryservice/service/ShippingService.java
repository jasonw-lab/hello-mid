package com.example.inventoryservice.service;

import com.example.inventoryservice.model.PaymentConfirmation;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Service;

import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for processing shipping based on payment confirmations.
 * This service manually consumes messages from Kafka and manages offsets explicitly.
 * 
 * This is used in the scenario: Payment Confirmation → Kafka → Shipping Notification
 * 
 * Manual consumption with explicit offset management is used because:
 * 1. We need precise control over when messages are considered "processed"
 * 2. We want to ensure "at-least-once" delivery semantics
 * 3. We need to handle retries and failures explicitly
 * 4. @KafkaListener is not suitable for this critical business process
 */
@Service
@Slf4j
public class ShippingService {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private KafkaConsumer<String, PaymentConfirmation> consumer;

    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        // Start the consumer in a separate thread
        executorService.submit(this::consumePaymentConfirmations);
    }

    @EventListener(ContextClosedEvent.class)
    public void destroy() {
        // Signal the consumer to stop
        running.set(false);
        if (consumer != null) {
            consumer.wakeup();
        }
        executorService.shutdown();
    }

    /**
     * Main method for consuming payment confirmations and processing shipping.
     * This method runs in a separate thread and continuously polls for new messages.
     */
    private void consumePaymentConfirmations() {
        // Set up consumer properties
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"); // Disable auto-commit
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.inventoryservice.model,com.example.paymentservice.model");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.example.inventoryservice.model.PaymentConfirmation");

        try {
            // Create consumer and subscribe to topic
            consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Collections.singletonList("payment-confirmation-topic"));
            log.info("Started consuming payment confirmations from payment-confirmation-topic");

            // Main consumption loop
            while (running.get()) {
                try {
                    // Poll for new messages with a timeout
                    ConsumerRecords<String, PaymentConfirmation> records = consumer.poll(Duration.ofMillis(100));
                    
                    // Process each record
                    for (ConsumerRecord<String, PaymentConfirmation> record : records) {
                        processPaymentConfirmation(record);
                        
                        // Manually commit offset for this record
                        commitOffset(record);
                    }
                } catch (Exception e) {
                    if (!(e instanceof WakeupException)) {
                        log.error("Error processing payment confirmations", e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in payment confirmation consumer", e);
        } finally {
            if (consumer != null) {
                consumer.close();
            }
            log.info("Payment confirmation consumer closed");
        }
    }

    /**
     * Process a payment confirmation and trigger shipping.
     * 
     * @param record The Kafka record containing the payment confirmation
     */
    private void processPaymentConfirmation(ConsumerRecord<String, PaymentConfirmation> record) {
        PaymentConfirmation paymentConfirmation = record.value();
        log.info("Processing payment confirmation: {}", paymentConfirmation);
        
        try {
            // Validate payment confirmation
            if (!"COMPLETED".equals(paymentConfirmation.getPaymentStatus())) {
                log.warn("Payment not completed for order: {}, status: {}", 
                        paymentConfirmation.getOrderId(), paymentConfirmation.getPaymentStatus());
                return;
            }
            
            // In a real application, this would:
            // 1. Look up the order details
            // 2. Check inventory availability
            // 3. Create a shipping record
            // 4. Notify the shipping department or third-party shipping provider
            
            // Simulate processing delay
            Thread.sleep(200);
            
            log.info("Shipping initiated for order: {}", paymentConfirmation.getOrderId());
        } catch (Exception e) {
            log.error("Error processing payment confirmation for order: {}", 
                    paymentConfirmation.getOrderId(), e);
            // In a production system, we might:
            // 1. Retry the operation
            // 2. Send to a dead letter queue
            // 3. Trigger an alert
        }
    }

    /**
     * Manually commit the offset for a processed record.
     * 
     * @param record The record to commit the offset for
     */
    private void commitOffset(ConsumerRecord<String, PaymentConfirmation> record) {
        try {
            // Create a map of TopicPartition to OffsetAndMetadata for the current record
            TopicPartition partition = new TopicPartition(record.topic(), record.partition());
            
            // Commit the offset for this specific partition
            // We add 1 to the offset because we want to commit the next offset to be consumed
            consumer.commitSync(Collections.singletonMap(
                    partition, 
                    new org.apache.kafka.clients.consumer.OffsetAndMetadata(record.offset() + 1)
            ));
            
            log.debug("Committed offset {} for partition {}", record.offset() + 1, partition);
        } catch (Exception e) {
            log.error("Error committing offset for record: {}", record, e);
        }
    }
}