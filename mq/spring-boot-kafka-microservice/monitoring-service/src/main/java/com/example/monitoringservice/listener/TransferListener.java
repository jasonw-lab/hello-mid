package com.example.monitoringservice.listener;

import com.example.monitoringservice.model.Transfer;
import com.example.monitoringservice.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferListener {

    private final MonitoringService monitoringService;

    @KafkaListener(topics = "monitor-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void handleTransfer(Transfer transfer) {
        log.info("Received transfer: {}", transfer);
        
        try {
            // Monitor the transfer
            monitoringService.monitorTransfer(transfer)
                .doOnSuccess(v -> log.info("Transfer monitoring completed for: {}", transfer.getTransferId()))
                .doOnError(e -> log.error("Error monitoring transfer: {}", transfer.getTransferId(), e))
                .subscribe();
        } catch (Exception e) {
            log.error("Error handling transfer message: {}", transfer, e);
        }
    }
}