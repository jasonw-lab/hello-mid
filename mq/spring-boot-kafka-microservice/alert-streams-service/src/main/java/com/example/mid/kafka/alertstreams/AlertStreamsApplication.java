package com.example.mid.kafka.alertstreams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.mid.kafka")
public class AlertStreamsApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlertStreamsApplication.class, args);
    }
}


