package com.example.monitoringservice.config;

import com.example.monitoringservice.model.Transfer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * monitoring-service の Kafka Streams 設定クラス。
 *
 * 主なポイント:
 * - キーは String、値は JSON Serde（Transfer を JSON としてシリアライズ/デシリアライズ）
 * - 処理保証 (processing.guarantee) をプロパティで切替可能にし、開発環境では at_least_once を既定化
 * - 異なるサービスから発行される Transfer の JSON を受け入れるため、信頼パッケージを設定
 * - @EnableKafkaStreams により、アプリ起動時に StreamsBuilderFactoryBean が生成され、
 *   TransferStreamProcessor の @Bean transferMonitoringTopology が自動で呼び出されます
 */
@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.streams.application-id:monitoring-service-streams}")
    private String applicationId;

    // 取引環境では EXACTLY_ONCE_V2 を推奨。開発環境では at_least_once を既定にしてトランザクション初期化失敗を回避
    @Value("${spring.kafka.streams.processing-guarantee:at_least_once}")
    private String processingGuarantee;

    // シングルブローカ構成でも内部トピックが作成できるように既定を 1 に
    @Value("${spring.kafka.streams.replication-factor:1}")
    private Integer replicationFactor;

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class.getName());
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, processingGuarantee);
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, replicationFactor);
        // Start consuming from earliest when no committed offset exists (align with TransferListener config)
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Do not halt the stream on deserialization errors; log and continue
        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.monitoringservice.model,com.example.transferservice.model");
        // Ignore type info headers so we don't require producer-side class on classpath
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public Serde<Transfer> transferSerde(ObjectMapper mapper) {
        JsonSerializer<Transfer> serializer = new JsonSerializer<>(mapper);
        // Disable use of type info headers to avoid class name mismatch across services
        //デフォルト（true）：ヘッダの __TypeId__ を見て、動的にクラスを判断。
        //false：ヘッダ無視、固定クラス（Transfer）でデシリアライズ。
        JsonDeserializer<Transfer> deserializer = new JsonDeserializer<>(Transfer.class, false);
        deserializer.addTrustedPackages("com.example.monitoringservice.model", "com.example.transferservice.model");
        return Serdes.serdeFrom(serializer, deserializer);
    }
}
