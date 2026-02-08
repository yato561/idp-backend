#!/bin/bash

# Rate Limit Testing Script (Alternative to Postman GUI)
# Usage: ./test-rate-limit.sh [service|admin] [requests] [delay_ms]

ENDPOINT=${1:-service}
NUM_REQUESTS=${2:-100}
DELAY_MS=${3:-0}

BASE_URL="http://localhost:8080"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "======================================"
echo "Rate Limit Testing Script"
echo "======================================"
echo "Endpoint: $ENDPOINT"
echo "Requests: $NUM_REQUESTS"
echo "Delay: ${DELAY_MS}ms"
echo ""

# Counter for results
allowed=0
limited=0
errors=0

if [ "$ENDPOINT" = "admin" ]; then
    echo "Testing ADMIN endpoints..."
    echo ""
    
    # You need to get a real ADMIN token first
    ADMIN_TOKEN="your_admin_jwt_token_here"
    
    for i in $(seq 1 $NUM_REQUESTS); do
        RESPONSE=$(curl -s -w "\n%{http_code}" \
            -H "Authorization: Bearer $ADMIN_TOKEN" \
            -H "Content-Type: application/json" \
            -d '{"serviceName":"test","version":"1.0.0"}' \
            "$BASE_URL/api/deployments")
        
        STATUS=$(echo "$RESPONSE" | tail -n1)
        BODY=$(echo "$RESPONSE" | head -n-1)
        
        if [ "$STATUS" = "429" ]; then
            echo -e "${i}: ${RED}✗ RATE LIMITED${NC} (429)"
            ((limited++))
        elif [ "$STATUS" = "200" ] || [ "$STATUS" = "201" ]; then
            echo -e "${i}: ${GREEN}✓ ALLOWED${NC} ($STATUS)"
            ((allowed++))
        else
            echo -e "${i}: ${YELLOW}⚠ ERROR${NC} ($STATUS)"
            ((errors++))
        fi
        
        if [ $DELAY_MS -gt 0 ]; then
            sleep 0.$(printf "%03d" $DELAY_MS)
        fi
    done
    
elif [ "$ENDPOINT" = "service" ]; then
    echo "Testing SERVICE endpoints (metrics/ingest)..."
    echo ""
    
    SERVICE_ID="service-001"
    
    for i in $(seq 1 $NUM_REQUESTS); do
        RESPONSE=$(curl -s -w "\n%{http_code}" \
            -H "X-Service-Id: $SERVICE_ID" \
            -H "Content-Type: application/json" \
            -d '{"metrics":[{"name":"cpu","value":45.5}]}' \
            "$BASE_URL/api/metrics/ingest")
        
        STATUS=$(echo "$RESPONSE" | tail -n1)
        BODY=$(echo "$RESPONSE" | head -n-1)
        
        if [ "$STATUS" = "429" ]; then
            echo -e "${i}: ${RED}✗ RATE LIMITED${NC} (429)"
            ((limited++))
        elif [ "$STATUS" = "200" ] || [ "$STATUS" = "201" ]; then
            echo -e "${i}: ${GREEN}✓ ALLOWED${NC} ($STATUS)"
            ((allowed++))
        else
            echo -e "${i}: ${YELLOW}⚠ ERROR${NC} ($STATUS)"
            ((errors++))
        fi
        
        if [ $DELAY_MS -gt 0 ]; then
            sleep 0.$(printf "%03d" $DELAY_MS)
        fi
    done
else
    echo "Usage: $0 [service|admin] [num_requests] [delay_ms]"
    echo ""
    echo "Examples:"
    echo "  $0 service 100 0      # 100 service requests, no delay"
    echo "  $0 admin 50 1000      # 50 admin requests, 1s delay"
    echo "  $0 service 200 100    # 200 requests, 100ms delay"
    exit 1
fi

echo ""
echo "======================================"
echo "Results Summary"
echo "======================================"
echo -e "${GREEN}Allowed:${NC}       $allowed"
echo -e "${RED}Rate Limited:${NC}  $limited"
echo -e "${YELLOW}Errors:${NC}        $errors"
echo "Total:        $NUM_REQUESTS"
echo ""
echo "First rate limit hit at request: ~$((allowed + 1))"
