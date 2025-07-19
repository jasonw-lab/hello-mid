# Java Local Cache Implementations

This project demonstrates three different implementations of local caching mechanisms in Java:

1. Custom ConcurrentHashMap-based Cache
2. Guava LoadingCache
3. Caffeine Cache

## 1. Custom ConcurrentHashMap-based Cache

A simple cache implementation using Java's built-in `ConcurrentHashMap`. This implementation provides basic caching functionality with optional expiration.

### Features:
- Thread-safe operations using ConcurrentHashMap
- Configurable expiration time for cache entries
- Automatic cleanup of expired entries
- Function-based value loading for cache misses

### Usage Example:
```java
// Create a cache with 5 seconds expiration time
CustomConcurrentMapCache<String, String> cache = 
    new CustomConcurrentMapCache<>(TimeUnit.SECONDS.toMillis(5));

// Put a value in the cache
cache.put("key1", "value1");

// Get a value (returns the cached value or computes it using the provided function)
String value = cache.get("key1", k -> "default");

// Put a value with custom expiration time (2 seconds)
cache.put("key2", "value2", 2000);

// Remove a value
String removed = cache.remove("key1");

// Clear the cache
cache.clear();

// Get the current size of the cache
int size = cache.size();
```

## 2. Guava LoadingCache

A cache implementation using Google Guava's LoadingCache. This implementation provides caching functionality with automatic loading, expiration, and size constraints.

### Features:
- Automatic value loading using a CacheLoader
- Maximum size constraint
- Automatic expiration after write
- Cache statistics recording

### Usage Example:
```java
// Create a Guava cache with default settings
GuavaLoadingCache<String, String> cache = 
    new GuavaLoadingCache<>(key -> "computed-" + key);

// Get a value (automatically loaded if not present)
String value = cache.get("key1");

// Put a value directly
cache.put("key2", "value2");

// Invalidate a specific entry
cache.invalidate("key2");

// Invalidate all entries
cache.invalidateAll();

// Get cache statistics
String stats = cache.stats();
```

## 3. Caffeine Cache

A cache implementation using Caffeine Cache. This implementation provides high-performance caching with features like automatic loading, expiration, and size constraints.

### Features:
- High-performance caching
- Maximum size constraint
- Automatic expiration after write
- Cache statistics recording

### Usage Example:
```java
// Create a Caffeine cache with default settings
CaffeineCache<String, String> cache = new CaffeineCache<>();

// Put a value in the cache
cache.put("key1", "value1");

// Get a value if present (returns null if not present)
String value = cache.getIfPresent("key1");

// Get a value with computation function
String computedValue = cache.get("key2", k -> "computed-" + k);

// Invalidate a specific entry
cache.invalidate("key1");

// Invalidate all entries
cache.invalidateAll();

// Get the estimated size
long size = cache.estimatedSize();

// Get cache statistics
String stats = cache.stats();
```

## Comparison of Cache Implementations

| Feature | Custom ConcurrentMap | Guava LoadingCache | Caffeine Cache |
|---------|---------------------|-------------------|----------------|
| Implementation | Java built-in | Google Guava | Caffeine |
| Performance | Good | Better | Best |
| Automatic Loading | Yes (via function) | Yes (via CacheLoader) | Yes (via function) |
| Expiration | Yes | Yes | Yes |
| Size Limit | No | Yes | Yes |
| Statistics | No | Yes | Yes |
| Memory Efficiency | Good | Better | Best |
| Complexity | Low | Medium | Medium |

## When to Use Each Implementation

- **Custom ConcurrentMapCache**: When you need a simple, lightweight cache with minimal dependencies.
- **Guava LoadingCache**: When you need more advanced features and already use Guava in your project.
- **Caffeine Cache**: When you need the highest performance and advanced caching features.

## Testing

See `CacheTest.java` for examples of how to use each cache implementation.