package com.example.useractivityservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableKafkaStreams
@EnableRetry
public class UserActivityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserActivityServiceApplication.class, args);
    }
}