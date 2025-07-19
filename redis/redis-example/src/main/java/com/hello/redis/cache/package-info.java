/**
 * This package provides three different implementations of local caching mechanisms in Java:
 *
 * <h2>1. Custom ConcurrentHashMap-based Cache</h2>
 *
 * A simple cache implementation using Java's built-in {@code ConcurrentHashMap}.
 * This implementation provides basic caching functionality with optional expiration.
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Thread-safe operations using ConcurrentHashMap</li>
 *   <li>Configurable expiration time for cache entries</li>
 *   <li>Automatic cleanup of expired entries</li>
 *   <li>Function-based value loading for cache misses</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Create a cache with 5 seconds expiration time
 * CustomConcurrentMapCache<String, String> cache =
 *     new CustomConcurrentMapCache<>(TimeUnit.SECONDS.toMillis(5));
 *
 * // Put a value in the cache
 * cache.put("key1", "value1");
 *
 * // Get a value (returns the cached value or computes it using the provided function)
 * String value = cache.get("key1", k -> "default");
 *
 * // Put a value with custom expiration time (2 seconds)
 * cache.put("key2", "value2", 2000);
 *
 * // Remove a value
 * String removed = cache.remove("key1");
 *
 * // Clear the cache
 * cache.clear();
 *
 * // Get the current size of the cache
 * int size = cache.size();
 * }</pre>
 *
 * <h2>2. Guava LoadingCache</h2>
 *
 * A cache implementation using Google Guava's LoadingCache.
 * This implementation provides caching functionality with automatic loading, expiration, and size constraints.
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Automatic value loading using a CacheLoader</li>
 *   <li>Maximum size constraint</li>
 *   <li>Automatic expiration after write</li>
 *   <li>Cache statistics recording</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Create a Guava cache with default settings
 * GuavaLoadingCache<String, String> cache =
 *     new GuavaLoadingCache<>(key -> "computed-" + key);
 *
 * // Get a value (automatically loaded if not present)
 * String value = cache.get("key1");
 *
 * // Put a value directly
 * cache.put("key2", "value2");
 *
 * // Invalidate a specific entry
 * cache.invalidate("key2");
 *
 * // Invalidate all entries
 * cache.invalidateAll();
 *
 * // Get cache statistics
 * String stats = cache.stats();
 * }</pre>
 *
 * <h2>3. Caffeine Cache</h2>
 *
 * A cache implementation using Caffeine Cache.
 * This implementation provides high-performance caching with features like automatic loading, expiration, and size constraints.
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>High-performance caching</li>
 *   <li>Maximum size constraint</li>
 *   <li>Automatic expiration after write</li>
 *   <li>Cache statistics recording</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Create a Caffeine cache with default settings
 * CaffeineCache<String, String> cache = new CaffeineCache<>();
 *
 * // Put a value in the cache
 * cache.put("key1", "value1");
 *
 * // Get a value if present (returns null if not present)
 * String value = cache.getIfPresent("key1");
 *
 * // Get a value with computation function
 * String computedValue = cache.get("key2", k -> "computed-" + k);
 *
 * // Invalidate a specific entry
 * cache.invalidate("key1");
 *
 * // Invalidate all entries
 * cache.invalidateAll();
 *
 * // Get the estimated size
 * long size = cache.estimatedSize();
 *
 * // Get cache statistics
 * String stats = cache.stats();
 * }</pre>
 *
 * <h2>Comparison of Cache Implementations</h2>
 *
 * <table>
 *   <tr>
 *     <th>Feature</th>
 *     <th>Custom ConcurrentMap</th>
 *     <th>Guava LoadingCache</th>
 *     <th>Caffeine Cache</th>
 *   </tr>
 *   <tr>
 *     <td>Implementation</td>
 *     <td>Java built-in</td>
 *     <td>Google Guava</td>
 *     <td>Caffeine</td>
 *   </tr>
 *   <tr>
 *     <td>Performance</td>
 *     <td>Good</td>
 *     <td>Better</td>
 *     <td>Best</td>
 *   </tr>
 *   <tr>
 *     <td>Automatic Loading</td>
 *     <td>Yes (via function)</td>
 *     <td>Yes (via CacheLoader)</td>
 *     <td>Yes (via function)</td>
 *   </tr>
 *   <tr>
 *     <td>Expiration</td>
 *     <td>Yes</td>
 *     <td>Yes</td>
 *     <td>Yes</td>
 *   </tr>
 *   <tr>
 *     <td>Size Limit</td>
 *     <td>No</td>
 *     <td>Yes</td>
 *     <td>Yes</td>
 *   </tr>
 *   <tr>
 *     <td>Statistics</td>
 *     <td>No</td>
 *     <td>Yes</td>
 *     <td>Yes</td>
 *   </tr>
 *   <tr>
 *     <td>Memory Efficiency</td>
 *     <td>Good</td>
 *     <td>Better</td>
 *     <td>Best</td>
 *   </tr>
 *   <tr>
 *     <td>Complexity</td>
 *     <td>Low</td>
 *     <td>Medium</td>
 *     <td>Medium</td>
 *   </tr>
 * </table>
 *
 * <h2>When to Use Each Implementation</h2>
 *
 * <ul>
 *   <li><b>Custom ConcurrentMapCache</b>: When you need a simple, lightweight cache with minimal dependencies.</li>
 *   <li><b>Guava LoadingCache</b>: When you need more advanced features and already use Guava in your project.</li>
 *   <li><b>Caffeine Cache</b>: When you need the highest performance and advanced caching features.</li>
 * </ul>
 *
 * <h2>Testing</h2>
 *
 * See {@code CacheTest.java} for examples of how to use each cache implementation.
 *
 * @see com.hello.redis.cache.CustomConcurrentMapCache
 * @see com.hello.redis.cache.GuavaLoadingCache
 * @see com.hello.redis.cache.CaffeineCache
 */
package com.hello.redis.cache;
