package com.example.useractivityservice.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${user-activity.input-topic}")
    private String inputTopic;

    @Value("${user-activity.output-topic}")
    private String outputTopic;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic inputTopic() {
        return TopicBuilder.name(inputTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic outputTopic() {
        return TopicBuilder.name(outputTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}