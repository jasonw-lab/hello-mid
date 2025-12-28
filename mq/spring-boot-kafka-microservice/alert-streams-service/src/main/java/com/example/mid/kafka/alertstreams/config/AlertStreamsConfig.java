package com.example.mid.kafka.alertstreams.config;

import com.example.mid.kafka.eventcontracts.TopicNames;
import com.example.mid.kafka.alertstreams.transform.OrderPaymentTransformer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

/**
 * Streams topology configuration.
 *
 * - Creates a persistent KeyValueStore used by the Transformer to track per-order state.
 * - Merges orders and payments streams into a single stream and delegates to the Transformer.
 *
 * Note: For the PoC we keep Serdes.String() for key/value (JSON string payloads).
 */
@Configuration
public class AlertStreamsConfig {
    public static final String STORE_NAME = "order-payment-store";

    @Bean
    public KStream<String, String> topology(StreamsBuilder builder) {
        // StateStore holds OrderPaymentState as JSON string values (RocksDB by default).
        StoreBuilder<KeyValueStore<String, String>> storeBuilder =
                Stores.keyValueStoreBuilder(
                        Stores.persistentKeyValueStore(STORE_NAME),
                        Serdes.String(),
                        Serdes.String()
                );
        builder.addStateStore(storeBuilder);

        KStream<String, String> orders = builder.stream(TopicNames.ORDERS);
        KStream<String, String> payments = builder.stream(TopicNames.PAYMENTS);

        // Merge and apply transformer which will emit alerts to the alerts topic.
        orders.merge(payments)
                .transform(() -> new OrderPaymentTransformer(STORE_NAME), STORE_NAME);

        // Return reference to orders stream (not used further in PoC)
        return orders;
    }
}


