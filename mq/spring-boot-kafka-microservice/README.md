# Spring Boot Kafka Examples

This repository contains two examples demonstrating the use of Apache Kafka with Spring Boot microservices.

## Example 1: Microservice Order Processing System

This example demonstrates a microservice architecture where services communicate asynchronously through Kafka:

- Order Service → (publishes to) → Kafka → (consumed by) → Payment Service, Inventory Service, Notification Service

The Kafka topic used is `order-topic`.

### Project Structure

#### Order Service
- Location: `/order-service`
- Function: Simulates order creation and sends order messages to Kafka

#### Payment Service
- Location: `/payment-service`
- Function: Consumes order messages and processes payments

#### Inventory Service
- Location: `/inventory-service`
- Function: Consumes order messages and updates inventory

#### Notification Service
- Location: `/notification-service`
- Function: Consumes order messages and sends notifications

## Example 2: Financial Monitoring System

This example demonstrates real-time data processing with Kafka for financial monitoring:

- Transfer Service → (publishes to) → Kafka → (consumed by) → Monitoring Service

The Kafka topic used is `monitor-topic`.

### Project Structure

#### Transfer Service
- Location: `/transfer-service`
- Function: Simulates user transfers and sends transfer data to Kafka

#### Monitoring Service
- Location: `/monitoring-service`
- Function: Consumes transfer messages, monitors transactions in real-time, and sends alerts when transfer amounts exceed thresholds

## Project Setup

To run these examples, you need to set up Kafka, Zookeeper, and Redis:

```
docker-compose up -d
```

To verify the installation:
```
docker ps
```

You should see Kafka, Zookeeper, and Redis containers running.

You can customize Redis password, Kafka settings, topic names, etc. by editing the [docker-compose.yml](./docker-compose.yml) file.

## Running the Examples

1. Make sure Redis, Zookeeper, and Apache Kafka are running
2. Ensure all application.yml files have configurations matching your docker-compose.yml setup
3. Run each service using `./gradlew bootRun` or through your IDE (IntelliJ or Eclipse)

## Author

- jason.w

## Repository

GitHub: https://github.com/jason-w/spring-boot-kafka-microservice