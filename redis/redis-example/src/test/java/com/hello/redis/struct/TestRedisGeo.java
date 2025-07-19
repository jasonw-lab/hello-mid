package com.hello.redis.struct;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.api.GeoEntry;
import org.redisson.api.GeoOrder;
import org.redisson.api.GeoPosition;
import org.redisson.api.GeoUnit;
import org.redisson.api.RGeo;
import org.redisson.api.RedissonClient;
import org.redisson.api.geo.GeoSearchArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class TestRedisGeo {

  @Autowired
  private RedissonClient redissonClient;

  @Test
  @EnabledIfEnvironmentVariable(named = "ENABLE_GEO_TESTS", matches = "true")
  public void testGeoOperations() {
    // Create a geo structure
    RGeo<String> geo = redissonClient.getGeo("hello:geo:test");

    // Add geo entries (cities with coordinates)
    geo.add(new GeoEntry(121.47, 31.21, "Shanghai"));
    geo.add(new GeoEntry(116.40, 39.90, "Beijing"));

    // Calculate distance between two cities
    double distance = geo.dist("Beijing", "Shanghai", GeoUnit.KILOMETERS);
    log.info("Distance between Beijing and Shanghai: {} km", distance);
    assertTrue(distance > 1000, "Distance should be more than 1000 km");

    // Get positions of cities
    Map<String, GeoPosition> positions = geo.pos("Beijing", "Shanghai");
    assertEquals(
      2,
      positions.size(),
      "Should return positions for both cities"
    );

    // Verify coordinates
    GeoPosition shanghaiPos = positions.get("Shanghai");
    assertNotNull(shanghaiPos, "Shanghai position should not be null");
    assertEquals(
      121.47,
      shanghaiPos.getLongitude(),
      0.1,
      "Shanghai longitude should be approximately 121.47"
    );
    assertEquals(
      31.21,
      shanghaiPos.getLatitude(),
      0.1,
      "Shanghai latitude should be approximately 31.21"
    );

    // Add more cities
    geo.add(new GeoEntry(120.16, 30.29, "Hangzhou"));
    geo.add(new GeoEntry(114.06, 22.54, "Shenzhen"));
    geo.add(new GeoEntry(113.26, 23.13, "Guangzhou"));

    // Search cities within radius from Shanghai
    GeoSearchArgs argsFromShanghai = GeoSearchArgs
      .from("Shanghai")
      .radius(300, GeoUnit.KILOMETERS)
      .order(GeoOrder.ASC)
      .count(10);

    Map<String, Double> citiesWithDistance = geo.searchWithDistance(
      argsFromShanghai
    );
    log.info("Cities within 300km of Shanghai: {}", citiesWithDistance);
    assertTrue(
      citiesWithDistance.containsKey("Hangzhou"),
      "Hangzhou should be within 300km of Shanghai"
    );
    assertFalse(
      citiesWithDistance.containsKey("Beijing"),
      "Beijing should not be within 300km of Shanghai"
    );

    // Search cities within radius from coordinates
    GeoSearchArgs argsFromCoordinates = GeoSearchArgs
      .from(113.5, 23.0)
      .radius(200, GeoUnit.KILOMETERS)
      .order(GeoOrder.ASC)
      .count(10);

    List<String> nearbyCities = geo.search(argsFromCoordinates);
    log.info(
      "Cities within 200km of coordinates (113.5, 23.0): {}",
      nearbyCities
    );
    assertTrue(
      nearbyCities.contains("Guangzhou"),
      "Guangzhou should be within 200km of the coordinates"
    );
    assertTrue(
      nearbyCities.contains("Shenzhen"),
      "Shenzhen should be within 200km of the coordinates"
    );

    // Clean up
    geo.delete();
  }
}
