package com.hello.redis.struct;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RHyperLogLog;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * <h1>Cardinality estimation</h1>
 * Similar to SQL distinct
 * HyperLogLog uses approximately 12KB memory
 * Error rate is around 0.81%
 */
@Slf4j
@SpringBootTest
@Testcontainers
public class TestRedisHyperLoglog {

  @Container
  static GenericContainer<?> redis = new GenericContainer<>(
    DockerImageName.parse("redis:7.0")
  )
    .withExposedPorts(6379);

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", redis::getFirstMappedPort);
  }

  @Autowired
  private RedissonClient redissonClient;

  @Test
  public void testHyperLogLogOperations() {
    // Create HyperLogLog for daily active users
    RHyperLogLog<Long> hyperLogLog = redissonClient.getHyperLogLog(
      "hello:hyperloglog:test"
    );

    // Add user IDs
    hyperLogLog.add(1L);
    hyperLogLog.add(10L);
    hyperLogLog.add(100L);
    hyperLogLog.addAll(Arrays.asList(1000L, 10000L));

    // Count unique users
    long uniqueUsers = hyperLogLog.count();
    log.info("Unique users count: {}", uniqueUsers);
    assertEquals(5, uniqueUsers, "Should have 5 unique users");

    // Add duplicate user ID
    hyperLogLog.add(1L);
    assertEquals(
      5,
      hyperLogLog.count(),
      "Count should not change when adding duplicate"
    );

    // Create another HyperLogLog for the next day
    RHyperLogLog<Long> nextDayHyperLogLog = redissonClient.getHyperLogLog(
      "hello:hyperloglog:test2"
    );
    nextDayHyperLogLog.add(1L);
    nextDayHyperLogLog.add(20L);
    nextDayHyperLogLog.add(200L);
    nextDayHyperLogLog.add(2000L);

    // Count unique users for the next day
    long nextDayUniqueUsers = nextDayHyperLogLog.count();
    log.info("Next day unique users count: {}", nextDayUniqueUsers);
    assertEquals(
      4,
      nextDayUniqueUsers,
      "Should have 4 unique users for the next day"
    );

    // Merge HyperLogLogs to get unique users across both days
    nextDayHyperLogLog.mergeWith("hello:hyperloglog:test");
    long combinedUniqueUsers = nextDayHyperLogLog.count();
    log.info("Combined unique users count: {}", combinedUniqueUsers);
    assertEquals(
      8,
      combinedUniqueUsers,
      "Should have 8 unique users across both days"
    );

    // Clean up
    hyperLogLog.delete();
    nextDayHyperLogLog.delete();
  }
}
