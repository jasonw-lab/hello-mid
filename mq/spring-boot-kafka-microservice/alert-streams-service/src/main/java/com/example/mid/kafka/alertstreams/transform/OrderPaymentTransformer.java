package com.example.mid.kafka.alertstreams.transform;

import com.example.mid.kafka.alertstreams.model.OrderPaymentState;
import com.example.mid.kafka.eventcontracts.TopicNames;
import com.example.mid.kafka.eventcontracts.ObjectMapperProvider;
import com.example.mid.kafka.eventcontracts.model.AlertRaisedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.processor.To;
import org.apache.kafka.streams.processor.AbstractProcessor;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

/**
 * Transformer implementation that:
 * - receives OrderConfirmed / PaymentSucceeded events (JSON) keyed by orderId
 * - maintains per-order OrderPaymentState in a KeyValueStore (persisted)
 * - on events updates deadlines or emits immediate alerts (rule C)
 * - uses a Punctuator (scheduled callback) to scan states and emit alerts for deadlines (rule A/B)
 *
 * Monitoring / Kafka notes:
 * - All input events are JSON strings; eventType identifies the type.
 * - Alerts are emitted to the topic defined in TopicNames.ALERTS (key=orderId, value=AlertRaised JSON).
 * - Punctuator frequency is set via punctuateIntervalMs (10s in PoC); shorter for testing.
 */
public class OrderPaymentTransformer extends AbstractProcessor<String, String> {
    private ProcessorContext context;
    private KeyValueStore<String, String> store;
    private final String storeName;
    private final ObjectMapper mapper = ObjectMapperProvider.get();
    private final long punctuateIntervalMs = Duration.ofSeconds(10).toMillis();

    public OrderPaymentTransformer(String storeName) {
        this.storeName = storeName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        // Acquire the state store (backed by RocksDB) provided in topology config
        this.store = (KeyValueStore<String, String>) context.getStateStore(storeName);

        // Schedule punctuator for scanning deadlines and emitting Rule A/B alerts.
        context.schedule(punctuateIntervalMs, PunctuationType.WALL_CLOCK_TIME, timestamp -> {
            try {
                long now = Instant.now().toEpochMilli();
                KeyValueIterator<String, String> iter = store.all();
                while (iter.hasNext()) {
                    KeyValue<String, String> kv = iter.next();
                    String key = kv.key;
                    String v = kv.value;
                    OrderPaymentState s = v == null ? new OrderPaymentState() : mapper.readValue(v, OrderPaymentState.class);
                    boolean changed = false;
                    // Rule A: Payment arrived but OrderConfirmed did not within T_confirm
                    if (s.ruleADeadlineEpochMs != null && !s.ruleAFired && now >= s.ruleADeadlineEpochMs && s.orderConfirmedAt == null) {
                        emitAlert(key, "A", "P2", s);
                        s.ruleAFired = true;
                        changed = true;
                    }
                    // Rule B: OrderConfirmed arrived but PaymentSucceeded did not within T_pay
                    if (s.ruleBDeadlineEpochMs != null && !s.ruleBFired && now >= s.ruleBDeadlineEpochMs && s.paymentSucceededAt == null) {
                        emitAlert(key, "B", "P2", s);
                        s.ruleBFired = true;
                        changed = true;
                    }
                    if (changed) {
                        store.put(key, mapper.writeValueAsString(s));
                    }
                }
                iter.close();
            } catch (Exception ex) {
                // In PoC we log/ignore parsing errors; in prod surface metrics/logs.
            }
        });
    }

    @Override
    public void process(String key, String value) {
        if (key == null || value == null) return;
        try {
            JsonNode node = mapper.readTree(value);
            String eventType = node.has("eventType") ? node.get("eventType").asText() : null;
            String occurredAt = node.has("occurredAt") ? node.get("occurredAt").asText() : Instant.now().toString();
            String raw = store.get(key);
            OrderPaymentState s = raw == null ? new OrderPaymentState() : mapper.readValue(raw, OrderPaymentState.class);

            if ("PaymentSucceeded".equals(eventType)) {
                // Increment counter and set first-seen timestamp
                s.paymentSuccessCount = s.paymentSuccessCount + 1;
                if (s.paymentSucceededAt == null) s.paymentSucceededAt = occurredAt;
                // Rule C: multiple payments for same order -> immediate high-severity alert
                if (s.paymentSuccessCount >= 2 && !s.ruleCFired) {
                    emitAlert(key, "C", "P1", s);
                    s.ruleCFired = true;
                }
                // If order not confirmed yet, set Rule A deadline (paymentSucceededAt + T_confirm)
                if (s.orderConfirmedAt == null && s.paymentSucceededAt != null) {
                    long deadline = Instant.parse(s.paymentSucceededAt).plusSeconds(30).toEpochMilli();
                    s.ruleADeadlineEpochMs = deadline;
                }
            } else if ("OrderConfirmed".equals(eventType)) {
                // Record confirmation time and set Rule B deadline if payment not yet seen
                if (s.orderConfirmedAt == null) s.orderConfirmedAt = occurredAt;
                if (s.paymentSucceededAt == null && s.orderConfirmedAt != null) {
                    long deadline = Instant.parse(s.orderConfirmedAt).plusSeconds(30).toEpochMilli();
                    s.ruleBDeadlineEpochMs = deadline;
                }
            }

            // Persist updated state
            store.put(key, mapper.writeValueAsString(s));
        } catch (Exception ex) {
            // ignore parse errors in PoC; add logging/metrics in production
        }
    }

    /**
     * Build and forward an AlertRaisedEvent (JSON) to the alerts topic.
     * For PoC we use ProcessorContext.forward with To.child(topic) to send to topic.
     */
    private void emitAlert(String orderId, String rule, String severity, OrderPaymentState s) {
        try {
            AlertRaisedEvent a = new AlertRaisedEvent();
            a.setAlertId(UUID.randomUUID().toString());
            a.setRule(rule);
            a.setSeverity(severity);
            a.setOrderId(orderId);
            a.setDetectedAt(Instant.now().toString());
            AlertRaisedEvent.Facts facts = new AlertRaisedEvent.Facts();
            facts.orderConfirmedAt = s.orderConfirmedAt;
            facts.paymentSucceededAt = s.paymentSucceededAt;
            facts.paymentSuccessCount = s.paymentSuccessCount;
            a.setFacts(facts);
            String json = mapper.writeValueAsString(a);
            // Forward to alerts topic (key=orderId)
            context.forward(orderId, json, To.child(TopicNames.ALERTS));
        } catch (Exception ex) {
            // ignore in PoC; production should record metrics/log errors
        }
    }

    @Override
    public void close() {
        // noop for PoC
    }
}


