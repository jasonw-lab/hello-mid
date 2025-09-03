package com.example.transferservice.controller;

import com.example.transferservice.model.Transfer;
import com.example.transferservice.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Slf4j
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Transfer> createTransfer(@RequestBody Transfer transfer) {
        log.info("Received transfer request: {}", transfer);
        return transferService.processTransfer(transfer);
    }
    
    @GetMapping("/sample")
    public Mono<Transfer> createSampleTransfer() {
        // Create a sample transfer for testing
        Transfer transfer = Transfer.builder()
                .transferId(UUID.randomUUID().toString())
                .fromAccount("ACC-" + (1000 + (int)(Math.random() * 9000)))
                .toAccount("ACC-" + (1000 + (int)(Math.random() * 9000)))
                .amount(new BigDecimal(100 + (int)(Math.random() * 900)))
                .transferDate(LocalDateTime.now())
                .status("COMPLETED")
                .description("Sample transfer")
                .build();
        
        log.info("Created sample transfer: {}", transfer);
        return transferService.processTransfer(transfer);
    }
    
    @GetMapping("/large-sample")
    public Mono<Transfer> createLargeSampleTransfer() {
        // Create a sample transfer with a large amount for testing the monitoring alert
        Transfer transfer = Transfer.builder()
                .transferId(UUID.randomUUID().toString())
                .fromAccount("ACC-" + (1000 + (int)(Math.random() * 9000)))
                .toAccount("ACC-" + (1000 + (int)(Math.random() * 9000)))
                .amount(new BigDecimal(5000 + (int)(Math.random() * 5000)))
                .transferDate(LocalDateTime.now())
                .status("COMPLETED")
                .description("Large sample transfer")
                .build();
        
        log.info("==== Created large sample transfer: {}", transfer);
        return transferService.processTransfer(transfer);
    }
}