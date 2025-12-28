package com.example.mid.kafka.eventcontracts.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderConfirmedEvent {
    @JsonProperty("eventType")
    private String eventType = "OrderConfirmed";

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("occurredAt")
    private String occurredAt;

    @JsonProperty("orderId")
    private String orderId;

    public OrderConfirmedEvent() {}

    public OrderConfirmedEvent(String eventId, String occurredAt, String orderId) {
        this.eventId = eventId;
        this.occurredAt = occurredAt;
        this.orderId = orderId;
    }

    public String getEventType() { return eventType; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getOccurredAt() { return occurredAt; }
    public void setOccurredAt(String occurredAt) { this.occurredAt = occurredAt; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}


