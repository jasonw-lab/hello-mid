package com.hello.redis.struct;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * Example class demonstrating Redis HyperLogLog operations.
 * This class shows the API and operations without requiring a running Redis instance.
 *
 * <h1>Cardinality estimation</h1>
 * Similar to SQL distinct
 * HyperLogLog uses approximately 12KB memory
 * Error rate is around 0.81%
 */
@Slf4j
public class TestRedisHyperLoglogExample {

  /**
   * Simple class to simulate HyperLogLog functionality.
   * In reality, HyperLogLog uses a probabilistic algorithm to estimate cardinality,
   * but for demonstration purposes, we'll use a HashSet to track exact cardinality.
   */
  static class SimpleHyperLogLog<T> {

    private final Set<T> elements = new HashSet<>();
    private final String name;

    public SimpleHyperLogLog(String name) {
      this.name = name;
    }

    public void add(T element) {
      elements.add(element);
    }

    public void addAll(Iterable<T> elementsToAdd) {
      for (T element : elementsToAdd) {
        elements.add(element);
      }
    }

    public long count() {
      return elements.size();
    }

    public void mergeWith(SimpleHyperLogLog<T> other) {
      this.elements.addAll(other.elements);
    }

    public String getName() {
      return name;
    }
  }

  /**
   * Demonstrates how to use Redis HyperLogLog operations.
   * In a real application, this would use a RedissonClient to interact with Redis.
   */
  @Test
  public void testHyperLogLogOperations() {
    log.info("HyperLogLog operations example");

    // In a real application with Redis, you would use:
    // RHyperLogLog<Long> hyperLogLog = redissonClient.getHyperLogLog("hello:hyperloglog:test");

    // For demonstration, we'll use our simple HyperLogLog implementation
    SimpleHyperLogLog<Long> hyperLogLog = new SimpleHyperLogLog<>(
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

    // Add duplicate user ID
    hyperLogLog.add(1L);
    log.info("Count after adding duplicate: {}", hyperLogLog.count());

    // Create another HyperLogLog for the next day
    SimpleHyperLogLog<Long> nextDayHyperLogLog = new SimpleHyperLogLog<>(
      "hello:hyperloglog:test2"
    );
    nextDayHyperLogLog.add(1L);
    nextDayHyperLogLog.add(20L);
    nextDayHyperLogLog.add(200L);
    nextDayHyperLogLog.add(2000L);

    // Count unique users for the next day
    long nextDayUniqueUsers = nextDayHyperLogLog.count();
    log.info("Next day unique users count: {}", nextDayUniqueUsers);

    // Merge HyperLogLogs to get unique users across both days
    nextDayHyperLogLog.mergeWith(hyperLogLog);
    long combinedUniqueUsers = nextDayHyperLogLog.count();
    log.info("Combined unique users count: {}", combinedUniqueUsers);
    // In Redis, this would be done with:
    // nextDayHyperLogLog.mergeWith("hello:hyperloglog:test");
    // long combinedUniqueUsers = nextDayHyperLogLog.count();
  }
}
