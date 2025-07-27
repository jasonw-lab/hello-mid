package com.example.useractivityservice.config;

import com.example.useractivityservice.model.PageView;
import com.example.useractivityservice.model.SuspiciousUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaStreamsConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.streams.application-id}")
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
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.useractivityservice.model");
        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public Serde<PageView> pageViewSerde(ObjectMapper mapper) {
        JsonSerializer<PageView> serializer = new JsonSerializer<>(mapper);
        JsonDeserializer<PageView> deserializer = new JsonDeserializer<>(PageView.class, mapper);
        deserializer.addTrustedPackages("com.example.useractivityservice.model");
        return Serdes.serdeFrom(serializer, deserializer);
    }

    @Bean
    public Serde<SuspiciousUser> suspiciousUserSerde(ObjectMapper mapper) {
        JsonSerializer<SuspiciousUser> serializer = new JsonSerializer<>(mapper);
        JsonDeserializer<SuspiciousUser> deserializer = new JsonDeserializer<>(SuspiciousUser.class, mapper);
        deserializer.addTrustedPackages("com.example.useractivityservice.model");
        return Serdes.serdeFrom(serializer, deserializer);
    }
}