package com.example.monitoringservice.service;

import com.example.monitoringservice.model.Transfer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@Slf4j
public class MonitoringService {

    @Value("${monitoring.threshold:1000}")
    private BigDecimal threshold;

    public Mono<Void> monitorTransfer(Transfer transfer) {
        log.info("Monitoring transfer: {}", transfer);
        
        // Check if the transfer amount exceeds the threshold
        if (transfer.getAmount().compareTo(threshold) > 0) {
            generateAlert(transfer);
        } else {
            log.info("Transfer amount is within normal range: {}", transfer.getAmount());
        }
        
        return Mono.empty();
    }
    
    private void generateAlert(Transfer transfer) {
        log.warn("ALERT: Large transfer detected!");
        log.warn("Transfer ID: {}", transfer.getTransferId());
        log.warn("From Account: {}", transfer.getFromAccount());
        log.warn("To Account: {}", transfer.getToAccount());
        log.warn("Amount: {}", transfer.getAmount());
        log.warn("Date: {}", transfer.getTransferDate());
        log.warn("Description: {}", transfer.getDescription());
        
        // In a real application, this could send an email, SMS, or notification to a monitoring system
        // For this example, we'll just log the alert
    }
}