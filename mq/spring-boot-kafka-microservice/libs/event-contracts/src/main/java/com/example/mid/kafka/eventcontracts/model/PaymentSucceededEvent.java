package com.example.mid.kafka.eventcontracts.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentSucceededEvent {
    @JsonProperty("eventType")
    private String eventType = "PaymentSucceeded";

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("occurredAt")
    private String occurredAt;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("paymentId")
    private String paymentId;

    @JsonProperty("provider")
    private String provider;

    @JsonProperty("amount")
    private Integer amount;

    @JsonProperty("currency")
    private String currency;

    public PaymentSucceededEvent() {}

    public String getEventType() { return eventType; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getOccurredAt() { return occurredAt; }
    public void setOccurredAt(String occurredAt) { this.occurredAt = occurredAt; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}


