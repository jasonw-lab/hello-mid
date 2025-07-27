package com.example.useractivityservice.controller;

import com.example.useractivityservice.model.PageView;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/pageviews")
@RequiredArgsConstructor
@Slf4j
public class PageViewController {

    private final KafkaTemplate<String, PageView> kafkaTemplate;

    @Value("${user-activity.input-topic}")
    private String topic;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimulationRequest {
        private String userId;
        private int count;
        private long delayMs = 0; // Default value of 0
    }

    @PostMapping
    public ResponseEntity<String> recordPageView(@RequestBody PageView pageView) {
        // Set timestamp if not provided
        if (pageView.getTimestamp() == null) {
            pageView.setTimestamp(LocalDateTime.now());
        }

        // Use userId as the key for the Kafka message
        String key = pageView.getUserId();
        
        log.info("Recording page view: {}", pageView);
        kafkaTemplate.send(topic, key, pageView);
        
        return ResponseEntity.ok("Page view recorded successfully");
    }

    @PostMapping("/simulate")
    public ResponseEntity<String> simulatePageViews(@RequestBody SimulationRequest request) {
        String userId = request.getUserId();
        int count = request.getCount();
        long delayMs = request.getDelayMs();
        
        log.info("Simulating {} page views for user {}", count, userId);
        
        for (int i = 0; i < count; i++) {
            PageView pageView = PageView.builder()
                    .userId(userId)
                    .sessionId(UUID.randomUUID().toString())
                    .pageUrl("https://example.com/page" + (i % 10))
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .ipAddress("192.168.1." + (i % 255))
                    .referrer("https://example.com")
                    .timestamp(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(topic, userId, pageView);
            
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        return ResponseEntity.ok(String.format("Successfully simulated %d page views for user %s", count, userId));
    }
    
    @GetMapping("/simulate")
    public ResponseEntity<String> simulatePageViewsWithQueryParams(
            @RequestParam String userId,
            @RequestParam int count,
            @RequestParam(required = false, defaultValue = "0") long delayMs) {
        
        log.info("Simulating {} page views for user {} with query parameters", count, userId);
        
        for (int i = 0; i < count; i++) {
            PageView pageView = PageView.builder()
                    .userId(userId)
                    .sessionId(UUID.randomUUID().toString())
                    .pageUrl("https://example.com/page" + (i % 10))
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .ipAddress("192.168.1." + (i % 255))
                    .referrer("https://example.com")
                    .timestamp(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(topic, userId, pageView);
            
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        return ResponseEntity.ok(String.format("Successfully simulated %d page views for user %s", count, userId));
    }
    
    @GetMapping("/simulate/{userId}/{count}")
    public ResponseEntity<String> simulatePageViews(
            @PathVariable String userId,
            @PathVariable int count,
            @RequestParam(required = false, defaultValue = "0") long delayMs) {
        
        log.info("Simulating {} page views for user {}", count, userId);
        
        for (int i = 0; i < count; i++) {
            PageView pageView = PageView.builder()
                    .userId(userId)
                    .sessionId(UUID.randomUUID().toString())
                    .pageUrl("https://example.com/page" + (i % 10))
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .ipAddress("192.168.1." + (i % 255))
                    .referrer("https://example.com")
                    .timestamp(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(topic, userId, pageView);
            
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        return ResponseEntity.ok(String.format("Successfully simulated %d page views for user %s", count, userId));
    }
}