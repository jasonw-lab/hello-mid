package com.example.monitoringservice.service;

import com.example.monitoringservice.model.Transfer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MonitoringService {

    @Value("${monitoring.threshold:1000}")
    private BigDecimal threshold;

    @Value("${monitoring.window-minutes:5}")
    private long windowMinutes;

    // Maintain a sliding window per fromAccount of recent transfers within the configured window
    private final Map<String, Deque<WindowEntry>> windows = new ConcurrentHashMap<>();

    public Mono<Void> monitorTransfer(Transfer transfer) {
        log.info("Monitoring transfer: {}", transfer);

        String account = transfer.getFromAccount();
        LocalDateTime now = transfer.getTransferDate() != null ? transfer.getTransferDate() : LocalDateTime.now();
        BigDecimal amount = transfer.getAmount() != null ? transfer.getAmount() : BigDecimal.ZERO;

        Deque<WindowEntry> deque = windows.computeIfAbsent(account, k -> new ArrayDeque<>());
        synchronized (deque) {
            // add current transfer
            deque.addLast(new WindowEntry(now, amount));
            // purge old entries
            purgeOld(now, deque);
            // compute sum
            BigDecimal sum = deque.stream().map(WindowEntry::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.compareTo(threshold) > 0) {
                generateAlert(transfer, sum);
            } else {
                log.info("Windowed amount within threshold. fromAccount={}, windowMinutes={}, sum={}, threshold={}", account, windowMinutes, sum, threshold);
            }
        }

        return Mono.empty();
    }

    private void purgeOld(LocalDateTime now, Deque<WindowEntry> deque) {
        Duration window = Duration.ofMinutes(windowMinutes);
        while (!deque.isEmpty()) {
            WindowEntry first = deque.peekFirst();
            if (first == null) break;
            if (Duration.between(first.time(), now).compareTo(window) > 0) {
                deque.removeFirst();
            } else {
                break;
            }
        }
    }

    private void generateAlert(Transfer transfer, BigDecimal windowSum) {
        log.warn("==== ALERT: High cumulative transfers detected within {} minutes!", windowMinutes);
        log.warn("Transfer ID: {}", transfer.getTransferId());
        log.warn("From Account: {}", transfer.getFromAccount());
        log.warn("To Account: {}", transfer.getToAccount());
        log.warn("Current Amount: {}", transfer.getAmount());
        log.warn("Window Sum: {} (threshold: {})", windowSum, threshold);
        log.warn("Date: {}", transfer.getTransferDate());
        log.warn("Description: {}", transfer.getDescription());
        // In a real application, this could send an alert via email/SMS/etc.
    }

    // Simple record to track timestamped amounts
    private record WindowEntry(LocalDateTime time, BigDecimal amount) {}
}