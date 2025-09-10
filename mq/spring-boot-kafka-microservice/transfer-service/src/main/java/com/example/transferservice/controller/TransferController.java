package com.example.transferservice.controller;

import com.example.transferservice.model.Transfer;
import com.example.transferservice.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    // 主要処理ポイント: REST 経由で受け取った Transfer をそのまま Kafka (monitor-topic) へ送信し、
    // 監視サービス（Kafka Streams）がリアルタイム集計・告警判定を行うデモの入口。
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Transfer> createTransfer(@RequestBody Transfer transfer) {
        log.info("Received transfer request: {}", transfer);
        return transferService.processTransfer(transfer);
    }

    // デモ用の簡易 API: 指定したパラメータで Transfer を作成して送信
    // 仕様の 'transferMoneyDemo(fromUser, toUser, currency, amount)' に対応。
    @RequestMapping(value = "/transferMoneyDemo", method = {RequestMethod.POST, RequestMethod.GET}, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Transfer> transferMoneyDemo(@RequestParam("fromUser") String fromUser,
                                            @RequestParam("toUser") String toUser,
                                            @RequestParam("currency") String currency,
                                            @RequestParam("amount") BigDecimal amount) {
        Transfer transfer = Transfer.builder()
                .transferId(UUID.randomUUID().toString())
                .fromAccount(fromUser) // fromUser -> fromAccount
                .toAccount(toUser)     // toUser   -> toAccount
                .amount(amount)
                .transferDate(LocalDateTime.now())
                .status("COMPLETED")
                .description("currency=" + currency)
                .build();
        log.info("[transferMoneyDemo] Built transfer from params: {}", transfer);
        return transferService.processTransfer(transfer);
    }
    
    @GetMapping(value = "/sample", produces = MediaType.APPLICATION_JSON_VALUE)
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
    
    @GetMapping(value = "/large-sample", produces = MediaType.APPLICATION_JSON_VALUE)
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