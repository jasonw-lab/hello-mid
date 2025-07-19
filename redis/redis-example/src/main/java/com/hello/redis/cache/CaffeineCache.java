package com.hello.redis.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * A cache implementation using Caffeine Cache.
 * This implementation provides high-performance caching with features like
 * automatic loading, expiration, and size constraints.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class CaffeineCache<K, V> {

  private final Cache<K, V> cache;

  /**
   * Creates a new Caffeine cache with the specified maximum size and expiration time.
   *
   * @param maximumSize the maximum number of entries the cache may contain
   * @param expireAfterWriteSeconds the duration after which entries should be automatically removed
   */
  public CaffeineCache(long maximumSize, long expireAfterWriteSeconds) {
    this.cache =
    Caffeine
      .newBuilder()
      .maximumSize(maximumSize)
      .expireAfterWrite(expireAfterWriteSeconds, TimeUnit.SECONDS)
      .recordStats()
      .build();
  }

  /**
   * Creates a new Caffeine cache with default settings:
   * - Maximum size: 1000 entries
   * - Expiration: 1 hour after write
   */
  public CaffeineCache() {
    this(1000, TimeUnit.HOURS.toSeconds(1));
  }

  /**
   * Returns the value associated with the given key, or computes it using the given function
   * if it's not present in the cache.
   *
   * @param key the key whose associated value is to be returned
   * @param mappingFunction the function to compute a value
   * @return the current (existing or computed) value associated with the specified key
   */
  public V get(K key, Function<K, V> mappingFunction) {
    return cache.get(key, mappingFunction);
  }

  /**
   * Returns the value associated with the given key, or null if there is no cached value.
   *
   * @param key the key whose associated value is to be returned
   * @return the value associated with the key, or null if not present
   */
  public V getIfPresent(K key) {
    return cache.getIfPresent(key);
  }

  /**
   * Associates the specified value with the specified key in this cache.
   * If the cache previously contained a value for the key, the old value is replaced.
   *
   * @param key the key with which the specified value is to be associated
   * @param value the value to be associated with the specified key
   */
  public void put(K key, V value) {
    cache.put(key, value);
  }

  /**
   * Discards any cached value for the key.
   *
   * @param key the key whose mapping is to be removed from the cache
   */
  public void invalidate(K key) {
    cache.invalidate(key);
  }

  /**
   * Discards all entries in the cache.
   */
  public void invalidateAll() {
    cache.invalidateAll();
  }

  /**
   * Returns the approximate number of entries in this cache.
   *
   * @return the approximate number of entries in this cache
   */
  public long estimatedSize() {
    return cache.estimatedSize();
  }

  /**
   * Returns a string representation of cache statistics.
   *
   * @return a string representation of cache statistics
   */
  public String stats() {
    return cache.stats().toString();
  }
}
