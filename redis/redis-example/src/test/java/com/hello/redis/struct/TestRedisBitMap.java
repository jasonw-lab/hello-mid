package com.hello.redis.struct;

import static org.junit.jupiter.api.Assertions.*;

import java.util.BitSet;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class TestRedisBitMap {

  @Autowired
  private RedissonClient redissonClient;

  @Test
  public void testBitMapOperations() {
    // Create BitMap for daily active users
    long userId = 1L;
    RBitSet dailyActiveUserBitMap = redissonClient.getBitSet(
      "hello:bitmap:test"
    );

    // Basic operations
    dailyActiveUserBitMap.set(userId);
    assertTrue(
      dailyActiveUserBitMap.get(userId),
      "User should be marked as active"
    );

    // Get count of bits set to true
    long trueCount = dailyActiveUserBitMap.cardinality();
    assertEquals(1, trueCount, "There should be exactly one active user");

    // Clear the bit
    dailyActiveUserBitMap.clear(userId);
    assertFalse(
      dailyActiveUserBitMap.get(userId),
      "User should no longer be active"
    );

    // Test with multiple users
    dailyActiveUserBitMap.set(1L);
    dailyActiveUserBitMap.set(2L);
    dailyActiveUserBitMap.set(3L);
    assertEquals(
      3,
      dailyActiveUserBitMap.cardinality(),
      "There should be 3 active users"
    );

    // Create another BitMap for the next day
    RBitSet nextDayActiveUserBitMap = redissonClient.getBitSet(
      "hello:bitmap:test2"
    );
    nextDayActiveUserBitMap.set(1L);
    nextDayActiveUserBitMap.set(4L);

    // Convert to Java BitSet for operations
    BitSet day1BitSet = dailyActiveUserBitMap.asBitSet();
    BitSet day2BitSet = nextDayActiveUserBitMap.asBitSet();

    // Perform AND operation (users active on both days)
    BitSet andResult = (BitSet) day1BitSet.clone();
    andResult.and(day2BitSet);
    assertEquals(
      1,
      andResult.cardinality(),
      "Only one user should be active on both days"
    );

    // Perform OR operation (users active on either day)
    BitSet orResult = (BitSet) day1BitSet.clone();
    orResult.or(day2BitSet);
    assertEquals(
      4,
      orResult.cardinality(),
      "Four unique users should be active across both days"
    );

    // Clean up
    dailyActiveUserBitMap.delete();
    nextDayActiveUserBitMap.delete();
  }
}
