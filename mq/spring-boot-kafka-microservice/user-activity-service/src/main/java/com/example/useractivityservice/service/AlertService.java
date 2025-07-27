package com.example.useractivityservice.service;

import com.example.useractivityservice.model.SuspiciousUser;

public interface AlertService {
    void sendAlert(SuspiciousUser suspiciousUser);
}