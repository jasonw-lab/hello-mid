package com.example.monitoringservice.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * AlertListener
 *
 * 役割: Kafka Streams が alert-topic に発行したアラートをシンプルにログ出力する。
 * テスト/デモ時に「告警消息 log 输出」の要件を満たすための補助コンポーネント。
 */
@Component
@Slf4j
public class AlertListener {

    // alert-topic は application.yml の monitoring.alert-topic から解決
    @KafkaListener(topics = "${monitoring.alert-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "stringKafkaListenerContainerFactory")
    public void onAlert(String alertPayload) {
        try {
            log.warn("[ALERT_TOPIC] Received alert: {}", alertPayload);
        } catch (Exception e) {
            log.error("[ALERT_TOPIC] Error handling alert payload: {}", alertPayload, e);
        }
    }
}
