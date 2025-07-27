package com.example.useractivityservice.stream;

import com.example.useractivityservice.model.PageView;
import com.example.useractivityservice.model.SuspiciousUser;
import com.example.useractivityservice.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
@EnableRetry
public class PageViewStreamProcessor {

    @Value("${user-activity.input-topic}")
    private String inputTopic;

    @Value("${user-activity.output-topic}")
    private String outputTopic;

    @Value("${user-activity.window-size-minutes}")
    private int windowSizeMinutes;

    @Value("${user-activity.page-view-threshold}")
    private long pageViewThreshold;

    private final Serde<PageView> pageViewSerde;
    private final Serde<SuspiciousUser> suspiciousUserSerde;
    private final AlertService alertService;
    private final KafkaAdmin kafkaAdmin;

    @Autowired
    private StreamsBuilder streamsBuilder;
    
    @PostConstruct
    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 5000, multiplier = 1.5))
    public void buildPipeline() {
        log.info("Building Kafka Streams pipeline with pageViewThreshold: {}", pageViewThreshold);
        // Check if topics exist before building the pipeline
        verifyTopicsExist();
        
        // Create a stream from the input topic
        KStream<String, PageView> pageViewStream = streamsBuilder
                .stream(inputTopic, Consumed.with(Serdes.String(), pageViewSerde))
                .peek((key, pageView) -> log.info("Received page view: {}", pageView));

        // Group by user ID
        KGroupedStream<String, PageView> groupedByUser = pageViewStream
                .groupByKey(Grouped.with(Serdes.String(), pageViewSerde));

        // Count page views in time windows
        TimeWindows timeWindows = TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(windowSizeMinutes));
        
        KTable<Windowed<String>, Long> windowedCounts = groupedByUser
                .windowedBy(timeWindows)
                .count();

        // Filter for suspicious users (exceeding threshold)
        KStream<String, SuspiciousUser> suspiciousUsers = windowedCounts
                .toStream()
                .peek((windowedKey, count) -> log.info("User {} has {} page views in {} minutes",
                        windowedKey.key(), count, windowSizeMinutes))
                .filter((windowedKey, count) -> {
                    boolean isSuspicious = count >= pageViewThreshold;
                    log.info("Is user {} suspicious? {}", windowedKey.key(), isSuspicious);
                    return isSuspicious;
                })
                .peek((windowedKey, count) -> log.warn("Suspicious activity detected: User {} has {} page views in {} minutes",
                        windowedKey.key(), count, windowSizeMinutes))
                .mapValues((windowedKey, count) -> {
                    // Get the last page view for this user to extract additional information
                    // In a real application, you might want to store more context about the user
                    return SuspiciousUser.builder()
                            .userId(windowedKey.key())
                            .ipAddress("unknown") // Set a default value to avoid null
                            .userAgent("unknown") // Set a default value to avoid null
                            .pageViewCount(count)
                            .windowStart(LocalDateTime.now().minusMinutes(windowSizeMinutes))
                            .windowEnd(LocalDateTime.now())
                            .detectionTime(LocalDateTime.now())
                            .reason("Excessive page views: " + count + " in " + windowSizeMinutes + " minutes")
                            .build();
                })
                .selectKey((windowedKey, suspiciousUser) -> suspiciousUser.getUserId());

        // Send suspicious users to the output topic
        suspiciousUsers
                .to(outputTopic, Produced.with(Serdes.String(), suspiciousUserSerde));

        // Also trigger alerts for suspicious users
        suspiciousUsers.foreach((key, suspiciousUser) -> {
            log.info("Calling alertService.sendAlert for user: {}", suspiciousUser.getUserId());
            try {
                alertService.sendAlert(suspiciousUser);
                log.info("Successfully sent alert for user: {}", suspiciousUser.getUserId());
            } catch (Exception e) {
                log.error("Error sending alert for user: {}", suspiciousUser.getUserId(), e);
            }
        });
        
        log.info("Kafka Streams topology built successfully");
    }

    /**
     * Verifies that the required topics exist before building the pipeline.
     * If topics don't exist, logs a warning but allows the application to continue.
     * Spring's KafkaAdmin will create the topics if they don't exist.
     */
    private void verifyTopicsExist() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            ListTopicsResult topics = adminClient.listTopics();
            KafkaFuture<Set<String>> topicNames = topics.names();
            Set<String> names = topicNames.get();
        
            if (!names.contains(inputTopic)) {
                log.warn("Input topic '{}' does not exist. It will be created automatically.", inputTopic);
            }
        
            if (!names.contains(outputTopic)) {
                log.warn("Output topic '{}' does not exist. It will be created automatically.", outputTopic);
            }
        
            log.info("Topics verification completed: input='{}', output='{}'", inputTopic, outputTopic);
        } catch (Exception e) {
            log.warn("Error verifying topics: {}. Continuing anyway as topics will be created automatically.", e.getMessage());
        }
    }
}