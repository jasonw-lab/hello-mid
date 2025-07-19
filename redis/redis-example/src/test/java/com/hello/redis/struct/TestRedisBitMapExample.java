package com.hello.redis.struct;

import java.util.BitSet;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * Example class demonstrating Redis BitMap operations.
 * This class shows the API and operations without requiring a running Redis instance.
 */
@Slf4j
public class TestRedisBitMapExample {

  /**
   * Demonstrates how to use Redis BitMap operations.
   * In a real application, this would use a RedissonClient to interact with Redis.
   */
  @Test
  public void testBitMapOperations() {
    log.info("BitMap operations example");

    // In a real application with Redis, you would use:
    // RBitSet dailyActiveUserBitMap = redissonClient.getBitSet("hello:bitmap:test");

    // For demonstration, we'll use Java's BitSet
    BitSet dailyActiveUserBitMap = new BitSet();

    // Basic operations
    long userId = 1L;
    dailyActiveUserBitMap.set((int) userId);
    log.info(
      "User {} is active: {}",
      userId,
      dailyActiveUserBitMap.get((int) userId)
    );

    // Get count of bits set to true
    long trueCount = dailyActiveUserBitMap.cardinality();
    log.info("Number of active users: {}", trueCount);

    // Clear the bit
    dailyActiveUserBitMap.clear((int) userId);
    log.info(
      "User {} is active after clearing: {}",
      userId,
      dailyActiveUserBitMap.get((int) userId)
    );

    // Test with multiple users
    dailyActiveUserBitMap.set(1);
    dailyActiveUserBitMap.set(2);
    dailyActiveUserBitMap.set(3);
    log.info(
      "Number of active users after adding 3 users: {}",
      dailyActiveUserBitMap.cardinality()
    );

    // Create another BitMap for the next day
    BitSet nextDayActiveUserBitMap = new BitSet();
    nextDayActiveUserBitMap.set(1);
    nextDayActiveUserBitMap.set(4);

    // Perform AND operation (users active on both days)
    BitSet andResult = (BitSet) dailyActiveUserBitMap.clone();
    andResult.and(nextDayActiveUserBitMap);
    log.info("Users active on both days: {}", andResult.cardinality());

    // Perform OR operation (users active on either day)
    BitSet orResult = (BitSet) dailyActiveUserBitMap.clone();
    orResult.or(nextDayActiveUserBitMap);
    log.info("Users active on either day: {}", orResult.cardinality());
  }
}
