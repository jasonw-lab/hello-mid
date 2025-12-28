package com.example.mid.kafka.eventcontracts.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AlertRaisedEvent {
    @JsonProperty("eventType")
    private String eventType = "AlertRaised";

    @JsonProperty("alertId")
    private String alertId;

    @JsonProperty("rule")
    private String rule;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("detectedAt")
    private String detectedAt;

    @JsonProperty("facts")
    private Facts facts;

    public static class Facts {
        @JsonProperty("orderConfirmedAt")
        public String orderConfirmedAt;
        @JsonProperty("paymentSucceededAt")
        public String paymentSucceededAt;
        @JsonProperty("paymentSuccessCount")
        public int paymentSuccessCount;
    }

    public AlertRaisedEvent() {}

    public String getEventType() { return eventType; }
    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }
    public String getRule() { return rule; }
    public void setRule(String rule) { this.rule = rule; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getDetectedAt() { return detectedAt; }
    public void setDetectedAt(String detectedAt) { this.detectedAt = detectedAt; }
    public Facts getFacts() { return facts; }
    public void setFacts(Facts facts) { this.facts = facts; }
}


