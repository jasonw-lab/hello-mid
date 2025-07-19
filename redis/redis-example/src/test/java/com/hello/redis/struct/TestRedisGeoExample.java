package com.hello.redis.struct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * Example class demonstrating Redis Geo operations.
 * This class shows the API and operations without requiring a running Redis instance.
 */
@Slf4j
public class TestRedisGeoExample {

  /**
   * Simple class to represent a geographic point with a name
   */
  @Data
  @AllArgsConstructor
  static class GeoPoint {

    private String name;
    private double longitude;
    private double latitude;

    /**
     * Calculate distance between two points using Haversine formula
     */
    public double distanceTo(GeoPoint other) {
      final int R = 6371; // Earth radius in kilometers

      double latDistance = Math.toRadians(other.latitude - this.latitude);
      double lonDistance = Math.toRadians(other.longitude - this.longitude);

      double a =
        Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
        Math.cos(Math.toRadians(this.latitude)) *
          Math.cos(Math.toRadians(other.latitude)) *
          Math.sin(lonDistance / 2) *
          Math.sin(lonDistance / 2);

      double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

      return R * c;
    }

    /**
     * Check if this point is within the specified radius of the center point
     */
    public boolean isWithinRadius(GeoPoint center, double radiusKm) {
      return distanceTo(center) <= radiusKm;
    }
  }

  /**
   * Demonstrates how to use Redis Geo operations.
   * In a real application, this would use a RedissonClient to interact with Redis.
   */
  @Test
  public void testGeoOperations() {
    log.info("Geo operations example");

    // In a real application with Redis, you would use:
    // RGeo<String> geo = redissonClient.getGeo("hello:geo:test");

    // For demonstration, we'll use a list of GeoPoint objects
    List<GeoPoint> geoPoints = new ArrayList<>();

    // Add geo entries (cities with coordinates)
    GeoPoint shanghai = new GeoPoint("Shanghai", 121.47, 31.21);
    GeoPoint beijing = new GeoPoint("Beijing", 116.47, 39.21);
    geoPoints.add(shanghai);
    geoPoints.add(beijing);

    // Calculate distance between two cities
    double distance = shanghai.distanceTo(beijing);
    log.info("Distance between Beijing and Shanghai: {} km", distance);

    // Get positions of cities
    Map<String, GeoPoint> positions = new HashMap<>();
    for (GeoPoint point : geoPoints) {
      positions.put(point.getName(), point);
    }
    log.info("Positions: {}", positions);

    // Add more cities
    geoPoints.add(new GeoPoint("Hangzhou", 120.16, 30.29));
    geoPoints.add(new GeoPoint("Shenzhen", 114.06, 22.54));
    geoPoints.add(new GeoPoint("Guangzhou", 113.26, 23.13));

    // Search cities within radius from Shanghai
    List<String> citiesNearShanghai = new ArrayList<>();
    Map<String, Double> citiesWithDistance = new HashMap<>();

    for (GeoPoint point : geoPoints) {
      double dist = shanghai.distanceTo(point);
      if (dist <= 300) { // 300km radius
        citiesNearShanghai.add(point.getName());
        citiesWithDistance.put(point.getName(), dist);
      }
    }

    log.info("Cities within 300km of Shanghai: {}", citiesWithDistance);

    // Search cities within radius from coordinates
    GeoPoint searchPoint = new GeoPoint("SearchPoint", 113.5, 23.0);
    List<String> nearbyCities = new ArrayList<>();

    for (GeoPoint point : geoPoints) {
      if (point.isWithinRadius(searchPoint, 200)) { // 200km radius
        nearbyCities.add(point.getName());
      }
    }

    log.info(
      "Cities within 200km of coordinates (113.5, 23.0): {}",
      nearbyCities
    );
  }
}
