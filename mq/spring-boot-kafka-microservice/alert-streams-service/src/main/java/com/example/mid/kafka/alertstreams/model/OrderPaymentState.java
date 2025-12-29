package com.example.mid.kafka.alertstreams.model;

/**
 * Per-order state kept in the KeyValueStore.
 *
 * Stored as a JSON string in the KeyValueStore for PoC simplicity.
 * Fields:
 * - orderConfirmedAt: ISO8601 timestamp when OrderConfirmed arrived (or null)
 * - paymentSucceededAt: ISO8601 timestamp when first PaymentSucceeded arrived (or null)
 * - paymentSuccessCount: number of PaymentSucceeded events observed
 * - ruleA/B deadlines and fired flags: used by Punctuator to detect timeouts
 *   and prevent duplicate alerts
 */
public class OrderPaymentState {
  public String orderConfirmedAt;
  public String paymentSucceededAt;
  public int paymentSuccessCount = 0;
  public Long ruleADeadlineEpochMs;
  public Long ruleBDeadlineEpochMs;
  public boolean ruleAFired = false;
  public boolean ruleBFired = false;
  public boolean ruleCFired = false;

  public OrderPaymentState() {}
}


