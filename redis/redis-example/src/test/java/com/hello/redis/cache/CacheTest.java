package com.hello.redis.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Test class to demonstrate the usage of different cache implementations.
 */
public class CacheTest {

  /**
   * Test the custom ConcurrentHashMap-based cache implementation.
   */
  @Test
  public void testCustomConcurrentMapCache() {
    // Create a cache with 5 seconds expiration time
    CustomConcurrentMapCache<String, String> cache =
      new CustomConcurrentMapCache<>(TimeUnit.SECONDS.toMillis(5));

    // Put a value in the cache
    cache.put("key1", "value1");

    // Get the value from the cache
    String value = cache.get("key1", k -> "default");
    assertEquals("value1", value);

    // Get a non-existent value, which should be computed using the provided function
    String defaultValue = cache.get("key2", k -> "default-" + k);
    assertEquals("default-key2", defaultValue);

    // Check the size of the cache
    assertEquals(2, cache.size());

    // Remove a value from the cache
    String removed = cache.remove("key1");
    assertEquals("value1", removed);

    // Check the size after removal
    assertEquals(1, cache.size());

    // Clear the cache
    cache.clear();

    // Check the size after clearing
    assertEquals(0, cache.size());

    // Test expiration
    cache.put("expiring", "value", 1000); // 1 second expiration

    // Value should be available immediately
    assertEquals("value", cache.get("expiring", k -> "expired"));

    try {
      // Wait for the value to expire
      Thread.sleep(1500);

      // Value should be recomputed after expiration
      assertEquals("expired", cache.get("expiring", k -> "expired"));
    } catch (InterruptedException e) {
      fail("Test interrupted");
    }
  }

  /**
   * Test the Guava LoadingCache implementation.
   */
  @Test
  public void testGuavaLoadingCache() {
    // Create a Guava cache with default settings
    GuavaLoadingCache<String, String> cache = new GuavaLoadingCache<>(key ->
      "computed-" + key
    );

    // Get a value, which should be computed using the loading function
    String value = cache.get("key1");
    assertEquals("computed-key1", value);

    // Put a value in the cache
    cache.put("key2", "value2");

    // Get the value from the cache
    String cachedValue = cache.get("key2");
    assertEquals("value2", cachedValue);

    // Invalidate a value
    cache.invalidate("key2");

    // The value should be recomputed after invalidation
    String recomputedValue = cache.get("key2");
    assertEquals("computed-key2", recomputedValue);

    // Check the size of the cache
    assertEquals(2, cache.size());

    // Invalidate all entries
    cache.invalidateAll();

    // Check the size after invalidation
    assertEquals(0, cache.size());
  }

  /**
   * Test the Caffeine Cache implementation.
   */
  @Test
  public void testCaffeineCache() {
    // Create a Caffeine cache with default settings
    CaffeineCache<String, String> cache = new CaffeineCache<>();

    // Put a value in the cache
    cache.put("key1", "value1");

    // Get the value from the cache
    String value = cache.getIfPresent("key1");
    assertEquals("value1", value);

    // Get a value with computation function
    String computedValue = cache.get("key2", k -> "computed-" + k);
    assertEquals("computed-key2", computedValue);

    // Get a non-existent value
    assertNull(cache.getIfPresent("key3"));

    // Invalidate a value
    cache.invalidate("key1");

    // The value should be gone after invalidation
    assertNull(cache.getIfPresent("key1"));

    // Check the size of the cache
    assertEquals(1, cache.estimatedSize());

    // Invalidate all entries
    cache.invalidateAll();

    // Check the size after invalidation
    assertEquals(0, cache.estimatedSize());
  }
}
