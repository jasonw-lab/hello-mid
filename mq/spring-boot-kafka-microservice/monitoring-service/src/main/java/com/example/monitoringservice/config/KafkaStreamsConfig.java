package com.example.monitoringservice.config;

import com.example.monitoringservice.model.Transfer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
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
 * - Exactly-Once 処理（EOS V2）を有効化し、障害時の重複アラート発行を防止
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
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.monitoringservice.model,com.example.transferservice.model");
        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public Serde<Transfer> transferSerde(ObjectMapper mapper) {
        JsonSerializer<Transfer> serializer = new JsonSerializer<>(mapper);
        JsonDeserializer<Transfer> deserializer = new JsonDeserializer<>(Transfer.class, mapper);
        deserializer.addTrustedPackages("com.example.monitoringservice.model", "com.example.transferservice.model");
        return Serdes.serdeFrom(serializer, deserializer);
    }
}
