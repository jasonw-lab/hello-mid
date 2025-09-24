#!/bin/bash

# Test script for User Activity Monitoring Service
# This script tests all three endpoints for simulating page views

echo "Starting User Activity Monitoring Service endpoint tests..."
echo "=========================================================="

# Base URL for the service
BASE_URL="http://localhost:8087/api/pageviews"

# Test 1: Original endpoint with path parameters
echo "Test 1: Testing original endpoint with path parameters"
echo "GET $BASE_URL/simulate/test-user1/30?delayMs=100"
curl -s -X GET "$BASE_URL/simulate/test-user1/30?delayMs=100"
echo -e "\n"

echo "Waiting 5 seconds..."
sleep 5

# Test 2: New GET endpoint with query parameters
echo "Test 2: Testing new GET endpoint with query parameters"
echo "GET $BASE_URL/simulate?userId=test-user2&count=40&delayMs=100"
curl -s -X GET "$BASE_URL/simulate?userId=test-user2&count=40&delayMs=100"
echo -e "\n"

echo "Waiting 5 seconds..."
sleep 5

# Test 3: New POST endpoint with request body
echo "Test 3: Testing new POST endpoint with request body"
echo "POST $BASE_URL/simulate"
curl -s -X POST "$BASE_URL/simulate" \
  -H "Content-Type: application/json" \
  -d '{"userId": "test-user3", "count": 50, "delayMs": 100}'
echo -e "\n"

echo "Test completed. All endpoints should have successfully simulated page views."
echo "Expected results:"
echo "- test-user1: 30 page views simulated via original endpoint"
echo "- test-user2: 40 page views simulated via new GET endpoint"
echo "- test-user3: 50 page views simulated via new POST endpoint"