package com.hello.redis.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * A simple cache implementation using ConcurrentHashMap.
 * This implementation provides basic caching functionality with optional expiration.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class CustomConcurrentMapCache<K, V> {

  private final ConcurrentHashMap<K, CacheEntry<V>> cache;
  private final long defaultExpirationTimeMillis;

  /**
   * Creates a new cache with default expiration time of 1 hour.
   */
  public CustomConcurrentMapCache() {
    this(TimeUnit.HOURS.toMillis(1));
  }

  /**
   * Creates a new cache with the specified default expiration time.
   *
   * @param defaultExpirationTimeMillis the default expiration time in milliseconds
   */
  public CustomConcurrentMapCache(long defaultExpirationTimeMillis) {
    this.cache = new ConcurrentHashMap<>();
    this.defaultExpirationTimeMillis = defaultExpirationTimeMillis;
  }

  /**
   * Returns the value associated with the given key, or computes it using the given function
   * if it's not present in the cache or has expired.
   *
   * @param key the key whose associated value is to be returned
   * @param mappingFunction the function to compute a value
   * @return the current (existing or computed) value associated with the specified key
   */
  public V get(K key, Function<K, V> mappingFunction) {
    cleanupExpiredEntries();

    CacheEntry<V> entry = cache.get(key);
    if (entry != null && !entry.isExpired()) {
      return entry.getValue();
    }

    V value = mappingFunction.apply(key);
    put(key, value);
    return value;
  }

  /**
   * Associates the specified value with the specified key in this cache.
   * If the cache previously contained a value for the key, the old value is replaced.
   *
   * @param key the key with which the specified value is to be associated
   * @param value the value to be associated with the specified key
   */
  public void put(K key, V value) {
    put(key, value, defaultExpirationTimeMillis);
  }

  /**
   * Associates the specified value with the specified key in this cache with a custom expiration time.
   * If the cache previously contained a value for the key, the old value is replaced.
   *
   * @param key the key with which the specified value is to be associated
   * @param value the value to be associated with the specified key
   * @param expirationTimeMillis the expiration time in milliseconds
   */
  public void put(K key, V value, long expirationTimeMillis) {
    long expirationTime = System.currentTimeMillis() + expirationTimeMillis;
    cache.put(key, new CacheEntry<>(value, expirationTime));
  }

  /**
   * Removes the mapping for a key from this cache if it is present.
   *
   * @param key the key whose mapping is to be removed from the cache
   * @return the previous value associated with the key, or null if there was no mapping
   */
  public V remove(K key) {
    CacheEntry<V> entry = cache.remove(key);
    return (entry != null) ? entry.getValue() : null;
  }

  /**
   * Removes all of the mappings from this cache.
   */
  public void clear() {
    cache.clear();
  }

  /**
   * Returns the number of key-value mappings in this cache.
   *
   * @return the number of key-value mappings in this cache
   */
  public int size() {
    cleanupExpiredEntries();
    return cache.size();
  }

  /**
   * Removes expired entries from the cache.
   */
  private void cleanupExpiredEntries() {
    cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
  }

  /**
   * A cache entry that holds a value and its expiration time.
   *
   * @param <V> the type of the value
   */
  private static class CacheEntry<V> {

    private final V value;
    private final long expirationTime;

    public CacheEntry(V value, long expirationTime) {
      this.value = value;
      this.expirationTime = expirationTime;
    }

    public V getValue() {
      return value;
    }

    public boolean isExpired() {
      return System.currentTimeMillis() > expirationTime;
    }
  }
}
