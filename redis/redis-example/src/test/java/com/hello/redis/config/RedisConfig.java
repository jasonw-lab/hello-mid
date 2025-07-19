package com.hello.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

  @Value("${spring.data.redis.host:localhost}")
  private String redisHost;

  @Value("${spring.data.redis.port:6379}")
  private int redisPort;
  
  @Value("${spring.data.redis.password:}")
  private String redisPassword;

  @Bean
  public RedissonClient redissonClient() {
    // Use in-memory Redis mode for testing
    Config config = new Config();
    config
      .useSingleServer()
      .setAddress("redis://" + redisHost + ":" + redisPort)
      .setConnectionMinimumIdleSize(1)
      .setConnectionPoolSize(1)
      .setDatabase(0)
      .setPassword(redisPassword);
    return Redisson.create(config);
  }
}
