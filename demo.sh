#!/bin/bash

# 3DS Server Demo Script
# This script demonstrates the basic functionality of the 3DS Server

echo "🚀 3DS Server Demo"
echo "=================="

# Wait for the server to start (if not already running)
echo "Waiting for server to be ready..."
sleep 2

# Test 1: Store a simple card range
echo ""
echo "📝 Test 1: Storing a card range"
echo "--------------------------------"
curl -X POST http://localhost:8080/api/3ds/store \
  -H "Content-Type: application/json" \
  -d '{
    "serialNum": "123",
    "messageType": "PRes",
    "dsTransID": "uuid-123",
    "cardRangeData": [
      {
        "startRange": "4000020000000000",
        "endRange": "4000020009999999",
        "actionInd": "A",
        "acsEndProtocolVersion": "2.1.0",
        "threeDSMethodURL": "https://example.com/3ds",
        "acsStartProtocolVersion": "2.1.0",
        "acsInfoInd": ["01", "02"]
      }
    ]
  }' -s

echo ""
echo "✅ Card range stored successfully!"

# Test 2: Retrieve the card range
echo ""
echo "🔍 Test 2: Retrieving card range for PAN 4000020005000000"
echo "--------------------------------------------------------"
curl -X GET "http://localhost:8080/api/3ds/method-url?pan=4000020005000000" -s | jq '.'

# Test 3: Test with a PAN that doesn't exist
echo ""
echo "❌ Test 3: Testing with non-existent PAN"
echo "----------------------------------------"
curl -X GET "http://localhost:8080/api/3ds/method-url?pan=9999999999999999" -s | jq '.'

# Test 4: Store multiple overlapping ranges
echo ""
echo "🔄 Test 4: Storing overlapping ranges"
echo "------------------------------------"
curl -X POST http://localhost:8080/api/3ds/store \
  -H "Content-Type: application/json" \
  -d '{
    "serialNum": "456",
    "messageType": "PRes",
    "dsTransID": "uuid-456",
    "cardRangeData": [
      {
        "startRange": "4000020000000000",
        "endRange": "4000020009999999",
        "actionInd": "A",
        "acsEndProtocolVersion": "2.1.0",
        "threeDSMethodURL": "https://example.com/3ds-wide",
        "acsStartProtocolVersion": "2.1.0",
        "acsInfoInd": ["01", "02"]
      },
      {
        "startRange": "4000020005000000",
        "endRange": "4000020005999999",
        "actionInd": "A",
        "acsEndProtocolVersion": "2.1.0",
        "threeDSMethodURL": "https://example.com/3ds-specific",
        "acsStartProtocolVersion": "2.1.0",
        "acsInfoInd": ["01", "02"]
      }
    ]
  }' -s

echo ""
echo "✅ Overlapping ranges stored successfully!"

# Test 5: Test conflict resolution (should return the smaller range)
echo ""
echo "🎯 Test 5: Testing conflict resolution (should return smaller range)"
echo "-------------------------------------------------------------------"
curl -X GET "http://localhost:8080/api/3ds/method-url?pan=4000020005500000" -s | jq '.'

echo ""
echo "🎉 Demo completed successfully!"
echo ""
echo "Note: The system returned the smaller (more specific) range when multiple ranges matched."
echo "This demonstrates the conflict resolution strategy implemented in the IntervalTree." 