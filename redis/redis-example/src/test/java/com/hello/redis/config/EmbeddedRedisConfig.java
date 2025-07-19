package com.hello.redis.config;

import org.springframework.context.annotation.Configuration;

/**
 * This configuration class is intentionally empty.
 *
 * Previously, this class was used to start an embedded Redis server for tests.
 * Now, tests are configured to use a local Redis server instead.
 *
 * Make sure you have a Redis server running locally on the default port (6379)
 * before running the tests.
 */
@Configuration
public class EmbeddedRedisConfig {
  // No embedded Redis server configuration
  // Tests will use the local Redis server
}
