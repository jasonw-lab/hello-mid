package com.example.transferservice;

import com.example.transferservice.model.Transfer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.util.*;

import static io.restassured.module.webtestclient.RestAssuredWebTestClient.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the transferMoneyDemo endpoint and stream-based alerting.
 *
 * What this test covers (as required by rule-kafka2.md):
 * - POST transferMoneyDemo with 500000 (below threshold) -> no alert
 * - POST transferMoneyDemo again with 500000 within 5 minutes -> alert produced to alert-topic
 * - Consume alert-topic and log alert payload
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@EmbeddedKafka(partitions = 1, topics = {"monitor-topic", "alert-topic"})
class TransferMoneyDemoIT {

    @LocalServerPort
    int port;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private WebTestClient webTestClient;

    private Consumer<String, String> alertConsumer;

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        // Use embedded Kafka for both the app producer and the test-local streams topology
        registry.add("spring.kafka.bootstrap-servers", () -> System.getProperty("spring.embedded.kafka.brokers"));
        // Monitoring defaults
        registry.add("monitoring.window-minutes", () -> "5");
        registry.add("monitoring.threshold", () -> "1000000");
    }

    @BeforeEach
    void setup() {
        // RestAssured + WebTestClient setup
        RestAssuredWebTestClient.webTestClient(webTestClient);
        RestAssuredWebTestClient.basePath = "/api/transfers";

        // Prepare consumer for alert-topic
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("alert-consumer-group", "true", embeddedKafka);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(), new StringDeserializer());
        alertConsumer = cf.createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(alertConsumer, "alert-topic");
    }

    @AfterEach
    void tearDown() {
        if (alertConsumer != null) {
            alertConsumer.close();
        }
    }

    @Test
    void transferMoneyDemo_twoCallsWithinWindow_producesAlertOnlyAfterSecond() {
        String fromUser = "user-it-001";
        String toUser1 = "merchant-001";
        String toUser2 = "merchant-002";

        // First call: 500000 -> below threshold, expect NO alert
        given()
            .queryParam("fromUser", fromUser)
            .queryParam("toUser", toUser1)
            .queryParam("currency", "CNY")
            .queryParam("amount", 500000)
        .when()
            .post("/transferMoneyDemo")
        .then()
            .statusCode(201);

        // Short poll: should be empty
        var records1 = KafkaTestUtils.getRecords(alertConsumer, Duration.ofSeconds(1));
        System.out.println("[DEBUG_LOG] Alerts after first call: count=" + records1.count());
        assertThat(records1.count()).isEqualTo(0);

        // Second call: +500000 -> reaches threshold (>= 1,000,000), expect ONE alert
        given()
            .queryParam("fromUser", fromUser)
            .queryParam("toUser", toUser2)
            .queryParam("currency", "CNY")
            .queryParam("amount", 500000)
        .when()
            .post("/transferMoneyDemo")
        .then()
            .statusCode(201);

        // Poll for alert up to 5 seconds
        ConsumerRecord<String, String> alert = null;
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline && alert == null) {
            var recs = KafkaTestUtils.getRecords(alertConsumer, Duration.ofMillis(500)).records("alert-topic");
            for (ConsumerRecord<String, String> r : recs) {
                alert = r;
                break;
            }
        }
        assertThat(alert).isNotNull();
        System.out.println("[DEBUG_LOG] Received alert: key=" + alert.key() + ", value=" + alert.value());
    }

    // Test-local Kafka Streams topology that mimics the monitoring-service behavior.
    @TestConfiguration
    @EnableKafkaStreams
    static class TestStreamsConfig {
        @Bean
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper;
        }

        @Bean(name = org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
        public KafkaStreamsConfiguration kStreamsConfig() {
            Map<String, Object> props = new HashMap<>();
            props.put(StreamsConfig.APPLICATION_ID_CONFIG, "transfer-it-streams");
            props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, System.getProperty("spring.embedded.kafka.brokers"));
            props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
            // Default value serde as String to avoid accidental JSON deserialization on internal topics
            props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
            return new KafkaStreamsConfiguration(props);
        }

        @Bean
        public Serde<Transfer> transferSerde(ObjectMapper mapper) {
            var serializer = new org.springframework.kafka.support.serializer.JsonSerializer<Transfer>(mapper);
            var deserializer = new JsonDeserializer<>(Transfer.class, mapper);
            deserializer.addTrustedPackages("*");
            return Serdes.serdeFrom(serializer, deserializer);
        }

        @Bean
        public KStream<String, String> testTopology(StreamsBuilder builder, Serde<Transfer> transferSerde) {
            KStream<String, String> raw = builder.stream("monitor-topic", Consumed.with(Serdes.String(), Serdes.String()));

            KStream<String, Transfer> source = raw
                    .mapValues(v -> {
                        try {
                            return objectMapper().readValue(v, Transfer.class);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter((k, v) -> v != null && v.getFromAccount() != null && v.getAmount() != null);

            TimeWindows windows = TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5));
            KTable<Windowed<String>, Double> aggregated = source
                    .selectKey((k, v) -> v.getFromAccount())
                    .groupByKey(Grouped.with(Serdes.String(), Serdes.serdeFrom(new org.springframework.kafka.support.serializer.JsonSerializer<>(), new JsonDeserializer<>(Transfer.class))))
                    .windowedBy(windows)
                    .aggregate(
                            () -> 0.0,
                            (key, value, aggregate) -> aggregate + (value.getAmount() == null ? 0.0 : value.getAmount().doubleValue()),
                            Materialized.with(Serdes.String(), Serdes.Double())
                    );

            KStream<String, String> alerts = aggregated
                    .toStream()
                    .filter((windowedKey, total) -> total != null && total >= 1_000_000)
                    .map((windowedKey, total) -> KeyValue.pair(windowedKey.key(), "ALERT:" + total));

            alerts.to("alert-topic", Produced.with(Serdes.String(), Serdes.String()));
            return alerts;
        }
    }
}
