package com.example.useractivityservice.service;

import com.example.useractivityservice.model.SuspiciousUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AlertServiceImpl implements AlertService {

    @Override
    public void sendAlert(SuspiciousUser suspiciousUser) {
        log.warn("SECURITY ALERT: Suspicious user activity detected!");
        log.warn("User ID: {}", suspiciousUser.getUserId());
        log.warn("IP Address: {}", suspiciousUser.getIpAddress());
        log.warn("User Agent: {}", suspiciousUser.getUserAgent());
        log.warn("Page View Count: {}", suspiciousUser.getPageViewCount());
        log.warn("Time Window: {} to {}", suspiciousUser.getWindowStart(), suspiciousUser.getWindowEnd());
        log.warn("Detection Time: {}", suspiciousUser.getDetectionTime());
        log.warn("Reason: {}", suspiciousUser.getReason());
        log.warn("-----------------------------------------------------");
        
        // In a real application, this could send an email, SMS, or notification to a monitoring system
        // For example:
        // emailService.sendEmail("security@example.com", "Suspicious Activity Alert", buildEmailContent(suspiciousUser));
        // smsService.sendSms("+1234567890", buildSmsContent(suspiciousUser));
        // slackService.sendNotification("#security-alerts", buildSlackMessage(suspiciousUser));
    }
}