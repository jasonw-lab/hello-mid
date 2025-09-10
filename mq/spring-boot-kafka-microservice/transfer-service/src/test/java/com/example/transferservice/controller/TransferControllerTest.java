package com.example.transferservice.controller;

import com.example.transferservice.model.Transfer;
import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@EmbeddedKafka(partitions = 1, topics = {"monitor-topic"})
@ExtendWith(OutputCaptureExtension.class)
class TransferControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void overrideKafkaBootstrapServers(DynamicPropertyRegistry registry) {
        // Use the broker addresses exposed by @EmbeddedKafka
        registry.add("spring.kafka.bootstrap-servers", () -> System.getProperty("spring.embedded.kafka.brokers"));
    }

    @BeforeEach
    void setUp() {
        // Bind RestAssured to Spring's WebTestClient provided by Spring Boot test context
        RestAssuredWebTestClient.webTestClient(webTestClient);
    }

    @Test
    @DisplayName("POST /api/transfers/transferMoneyDemo should create transfer with correct mapping and return 201")
    void transferMoneyDemo_createsTransfer_returns201(CapturedOutput output) {
        String fromUser = "alice";
        String toUser = "bob";
        String currency = "USD";
        BigDecimal amount = new BigDecimal("100");

        RestAssuredWebTestClient
                .given()
                .queryParam("fromUser", fromUser)
                .queryParam("toUser", toUser)
                .queryParam("currency", currency)
                .queryParam("amount", amount)
                .when()
                .post("/api/transfers/transferMoneyDemo")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("fromAccount", Matchers.equalTo(fromUser))
                .body("toAccount", Matchers.equalTo(toUser))
                .body("description", Matchers.equalTo("currency=" + currency))
                .body("transferId", Matchers.notNullValue())
                .body("amount", Matchers.notNullValue())
                .body("status", Matchers.equalTo("COMPLETED"));

        // Assert that the controller method logged its construction message
        assertThat(output.getOut()).contains("[transferMoneyDemo] Built transfer from params:");
    }
}
