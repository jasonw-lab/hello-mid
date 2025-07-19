package com.hello.redis.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * A cache implementation using Guava's LoadingCache.
 * This implementation provides caching functionality with automatic loading,
 * expiration, and size constraints.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class GuavaLoadingCache<K, V> {

  private final LoadingCache<K, V> cache;

  /**
   * Creates a new Guava cache with the specified loading function, maximum size, and expiration time.
   *
   * @param loadingFunction the function to compute values
   * @param maximumSize the maximum number of entries the cache may contain
   * @param expireAfterWriteSeconds the duration after which entries should be automatically removed
   */
  public GuavaLoadingCache(
    Function<K, V> loadingFunction,
    long maximumSize,
    long expireAfterWriteSeconds
  ) {
    CacheLoader<K, V> loader = new CacheLoader<K, V>() {
      @Override
      public V load(K key) {
        return loadingFunction.apply(key);
      }
    };

    this.cache =
    CacheBuilder
      .newBuilder()
      .maximumSize(maximumSize)
      .expireAfterWrite(expireAfterWriteSeconds, TimeUnit.SECONDS)
      .recordStats()
      .build(loader);
  }

  /**
   * Creates a new Guava cache with default settings:
   * - Maximum size: 1000 entries
   * - Expiration: 1 hour after write
   *
   * @param loadingFunction the function to compute values
   */
  public GuavaLoadingCache(Function<K, V> loadingFunction) {
    this(loadingFunction, 1000, TimeUnit.HOURS.toSeconds(1));
  }

  /**
   * Returns the value associated with the given key, loading that value if necessary.
   *
   * @param key the key whose associated value is to be returned
   * @return the value associated with the key
   * @throws RuntimeException if an exception was thrown while loading the value
   */
  public V get(K key) {
    try {
      return cache.get(key);
    } catch (ExecutionException e) {
      throw new RuntimeException("Error loading value for key: " + key, e);
    }
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
  public long size() {
    return cache.size();
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
