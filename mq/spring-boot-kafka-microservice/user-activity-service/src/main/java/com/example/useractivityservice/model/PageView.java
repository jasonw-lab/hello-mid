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
public class PageView {
    private String userId;
    private String sessionId;
    private String pageUrl;
    private String userAgent;
    private String ipAddress;
    private String referrer;
    private LocalDateTime timestamp;
}