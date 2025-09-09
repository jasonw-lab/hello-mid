package com.example.monitoringservice.stream;

import com.example.monitoringservice.config.KafkaStreamsConfig;
import com.example.monitoringservice.model.Transfer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.kstream.KStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Properties;

import static org.apache.kafka.streams.StreamsConfig.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {KafkaStreamsConfig.class, TransferStreamProcessor.class})
@TestPropertySource(properties = {
        "monitoring.window-minutes=5",
        "monitoring.threshold=1000000",
        "monitoring.input-topic=monitor-topic-test",
        "monitoring.alert-topic=alert-topic-test",
        "spring.kafka.streams.application-id=monitoring-service-streams-test",
        "spring.kafka.bootstrap-servers=dummy:9092"
})
class TransferStreamProcessorTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Serde<Transfer> transferSerde;

    @Autowired
    private TransferStreamProcessor processor;

    private TopologyTestDriver testDriver;
    private TestInputTopic<String, Transfer> inputTopic;
    private TestOutputTopic<String, String> alertTopic;

    @BeforeEach
    void setup() {
        // Build topology using the same bean method
        var builder = new org.apache.kafka.streams.StreamsBuilder();
        KStream<String, String> alertsStream = processor.transferMonitoringTopology(builder);
        Topology topology = builder.build();

        Properties props = new Properties();
        props.put(APPLICATION_ID_CONFIG, "monitoring-service-streams-test");
        props.put(BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        props.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class);

        testDriver = new TopologyTestDriver(topology, props);

        inputTopic = testDriver.createInputTopic(
                "monitor-topic-test",
                Serdes.String().serializer(),
                transferSerde.serializer()
        );
        alertTopic = testDriver.createOutputTopic(
                "alert-topic-test",
                Serdes.String().deserializer(),
                Serdes.String().deserializer()
        );
    }

    @AfterEach
    void tearDown() {
        if (testDriver != null) {
            testDriver.close();
        }
    }

    @Test
    void whenTwoTransfersWithinWindow_reachesThreshold_thenAlertProduced() {
        // Arrange: two transfers of 500,000 from the same account within 5 minutes
        Transfer t1 = Transfer.builder()
                .transferId("t1")
                .fromAccount("user-001")
                .toAccount("user-002")
                .amount(BigDecimal.valueOf(500000))
                .transferDate(LocalDateTime.now())
                .status("COMPLETED")
                .description("first half")
                .build();

        Transfer t2 = Transfer.builder()
                .transferId("t2")
                .fromAccount("user-001")
                .toAccount("user-003")
                .amount(BigDecimal.valueOf(500000))
                .transferDate(LocalDateTime.now().plusMinutes(1))
                .status("COMPLETED")
                .description("second half")
                .build();

        long startTs = 0L;
        // Act: pipe first message
        inputTopic.pipeInput("t1", t1, startTs);
        assertTrue(alertTopic.isEmpty(), "No alert expected after first transfer below threshold");

        // Pipe second message 1 minute later (still within 5-minute window)
        inputTopic.pipeInput("t2", t2, startTs + Duration.ofMinutes(1).toMillis());

        // Assert: one alert should be produced (>= threshold)
        assertFalse(alertTopic.isEmpty(), "Alert expected after cumulative amount reaches threshold");
        var record = alertTopic.readKeyValue();
        assertEquals("user-001", record.key);
        assertNotNull(record.value);
        assertTrue(record.value.contains("TRANSFER_THRESHOLD_EXCEEDED"));
        assertTrue(alertTopic.isEmpty(), "Only one alert expected in this simple scenario");
    }
}
