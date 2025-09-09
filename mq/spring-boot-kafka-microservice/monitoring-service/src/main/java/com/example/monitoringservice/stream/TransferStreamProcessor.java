package com.example.monitoringservice.stream;

import com.example.monitoringservice.model.Transfer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.StreamsBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * TransferStreamProcessor
 *
 * リアルタイムで振込(Transfer)イベントを Kafka Streams で監視するトポロジ定義。
 *
 * トポロジの概要:
 * - 入力: monitoring.input-topic（デフォルト: monitor-topic）から Transfer を消費
 * - 前処理: 不正なレコード（null、fromAccount 空白、amount null）を除外
 * - 集計: fromAccount 単位で監視ウィンドウ（monitoring.window-minutes）の合計金額を集計（タンブリング）
 * - アラート: ウィンドウ合計が monitoring.threshold を超過した場合に monitoring.alert-topic へ通知
 *
 * 出力のアラートは JSON（account,totalAmount,windowStart/End,type を含む）
 *
 * 呼び元について:
 * - 本メソッドは @Bean で定義されており、KafkaStreamsConfig の @EnableKafkaStreams により
 *   Spring Kafka Streams のインフラ（StreamsBuilderFactoryBean）がアプリ起動時に自動で呼び出し、
 *   StreamsBuilder を渡してトポロジを構築・起動します。アプリ側で明示的に呼び出すコードは不要です。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TransferStreamProcessor {

    @Value("${monitoring.input-topic:monitor-topic}")
    private String inputTopic;

    @Value("${monitoring.alert-topic:alert-topic}")
    private String alertTopic;

    @Value("${monitoring.window-minutes:5}")
    private int windowSizeMinutes;

    // デフォルト閾値は仕様に合わせて 1,000,000 に設定
    @Value("${monitoring.threshold:1000000}")
    private double threshold;

    private final ObjectMapper objectMapper;
    private final Serde<Transfer> transferSerde;

    /**
     * 重要な監視設定値を起動時に検証し、誤設定によるサイレントな不具合を避ける。
     */
    @PostConstruct
    void validateMonitoringConfig() {
        if (windowSizeMinutes <= 0) {
            log.warn("monitoring.window-minutes should be > 0, but was {}. Falling back to 5.", windowSizeMinutes);
            windowSizeMinutes = 5;
        }
        if (threshold <= 0) {
            log.warn("monitoring.threshold should be > 0, but was {}. Falling back to 1000.", threshold);
            threshold = 1000;
        }
    }

    @Bean
    public KStream<String, String> transferMonitoringTopology(StreamsBuilder streamsBuilder) {
        log.info("Building transfer monitoring topology with window {} minutes and threshold {}", windowSizeMinutes, threshold);

        // 設定済みの入力トピックから、キー:String・値:Transfer として読み込む
        KStream<String, Transfer> source = streamsBuilder.stream(
                inputTopic,
                Consumed.with(Serdes.String(), transferSerde)
        )
        // 開発環境での可観測性向上のために受信イベントをログ出力
        .peek((k, v) -> log.info("[Streams] Received transfer: {}", v))
        // グルーピング/集計での不具合を防ぐため、nullキーや金額を防御的に除外
        .filter((k, v) -> v != null && v.getFromAccount() != null && !v.getFromAccount().isBlank() && v.getAmount() != null);

        // グレース期間なしのタンブリングウィンドウ（遅延到着イベントは破棄）。遅延データを許容する場合は grace を検討
        TimeWindows windows = TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(windowSizeMinutes));

        // 監視ウィンドウ内で fromAccount 単位に金額の合計を集計
        KTable<Windowed<String>, Double> aggregated = source
                .selectKey((k, v) -> v.getFromAccount()) // fromAccount で再キー化
                .groupByKey(Grouped.with(Serdes.String(), /* 値Serdeは推論されるため null */ null))
                .windowedBy(windows)
                .aggregate(
                        () -> 0.0, // 初期値
                        (key, value, aggregate) -> aggregate + (value.getAmount() == null ? 0.0 : value.getAmount().doubleValue()),
                        Materialized.with(Serdes.String(), Serdes.Double())
                );

        // ウィンドウ合計が閾値を超過した場合にアラートを生成
        KStream<String, String> alerts = aggregated
                .toStream()
                // 閾値「以上」でアラート（ビジネス要件: 500000 x2 と閾値=1000000 で発火）
                .filter((windowedKey, total) -> total != null && total >= threshold)
                .map((windowedKey, total) -> {
                    String account = windowedKey.key();
                    long startMs = windowedKey.window().start();
                    long endMs = windowedKey.window().end();
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("type", "TRANSFER_THRESHOLD_EXCEEDED");
                    alert.put("fromAccount", account);
                    alert.put("totalAmount", total);
                    alert.put("windowStart", formatInstant(startMs));
                    alert.put("windowEnd", formatInstant(endMs));
                    String payload;
                    try {
                        payload = objectMapper.writeValueAsString(alert);
                    } catch (JsonProcessingException e) {
                        // JSON シリアライズに失敗した場合、アラートを捨てず安全に toString() へフォールバック
                        log.warn("Failed to serialize alert JSON, using toString(): {}", alert, e);
                        payload = alert.toString();
                    }
                    log.warn("[ALERT] Account {} exceeded threshold {} with total {} in window {}-{}", account, threshold, total, alert.get("windowStart"), alert.get("windowEnd"));
                    return KeyValue.pair(account, payload);
                });

        // アラートを設定済みのアラート用トピックへ送信
        alerts.to(alertTopic, Produced.with(Serdes.String(), Serdes.String()));

        return alerts; // 可観測性/テスト用途のため返却
    }

    private String formatInstant(long epochMs) {
        Instant instant = Instant.ofEpochMilli(epochMs);
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault()).format(instant);
    }
}
