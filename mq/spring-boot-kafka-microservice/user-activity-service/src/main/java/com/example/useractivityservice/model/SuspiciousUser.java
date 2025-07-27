package com.example.useractivityservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuspiciousUser {
    private String userId;
    private String ipAddress;
    private String userAgent;
    private long pageViewCount;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private LocalDateTime detectionTime;
    private String reason;
}