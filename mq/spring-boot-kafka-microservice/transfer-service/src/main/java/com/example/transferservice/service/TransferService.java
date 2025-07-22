package com.example.transferservice.service;

import com.example.transferservice.model.Transfer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final KafkaTemplate<String, Transfer> kafkaTemplate;

    public Mono<Transfer> processTransfer(Transfer transfer) {
        // Set transfer ID and date if not provided
        if (transfer.getTransferId() == null) {
            transfer.setTransferId(UUID.randomUUID().toString());
        }
        if (transfer.getTransferDate() == null) {
            transfer.setTransferDate(LocalDateTime.now());
        }
        
        // Set initial status if not provided
        if (transfer.getStatus() == null) {
            transfer.setStatus("COMPLETED");
        }
        
        log.info("Processing transfer: {}", transfer);
        
        // Send the transfer to Kafka
        kafkaTemplate.send("monitor-topic", transfer.getTransferId(), transfer)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Transfer sent to Kafka: {}", transfer.getTransferId());
                } else {
                    log.error("Failed to send transfer to Kafka: {}", ex.getMessage());
                }
            });
        
        return Mono.just(transfer);
    }
}