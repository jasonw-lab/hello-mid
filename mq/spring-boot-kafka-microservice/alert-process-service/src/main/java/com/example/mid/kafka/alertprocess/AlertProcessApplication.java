package com.example.mid.kafka.alertprocess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.mid.kafka")
public class AlertProcessApplication {
  public static void main(String[] args) {
    SpringApplication.run(AlertProcessApplication.class, args);
  }
}
