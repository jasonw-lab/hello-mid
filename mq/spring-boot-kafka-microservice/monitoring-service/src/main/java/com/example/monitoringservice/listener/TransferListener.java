package com.example.monitoringservice.listener;

import com.example.monitoringservice.model.Transfer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 開発/テスト環境での可観測性向上のためのシンプルなコンシューマ。
 * なお、リアルタイム監視とアラート生成の本体は Kafka Streams のトポロジで処理されます。
 */
@Component
@Slf4j
public class TransferListener {

    // 入力トピックは application.yml の monitoring.input-topic を使用（ハードコード禁止）
    @KafkaListener(topics = "${monitoring.input-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleTransfer(Transfer transfer) {
        // 予期しない例外でリスナーコンテナが落ちないよう、安全にログして処理します
        try {
            log.info("[Listener] Received transfer: {}", transfer);
        } catch (Exception e) {
            log.error("[Listener] Error handling transfer message: {}", transfer, e);
        }
    }
}