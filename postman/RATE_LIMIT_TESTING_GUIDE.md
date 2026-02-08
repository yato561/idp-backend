# Rate Limiting Testing Guide - Postman Runner

## Rate Limit Rules Reference

| Endpoint | Role | Capacity | Refill/sec | Description |
|----------|------|----------|-----------|-------------|
| `POST /api/deployments` | ADMIN | 100 tokens | 10/sec | ~10 sec to exhaust |
| `POST /api/logs/ingest` | ADMIN | 200 tokens | 20/sec | ~10 sec to exhaust |
| `POST /api/metrics/ingest` | SERVICE | 60 tokens | 5/sec | ~12 sec to exhaust |
| `POST /api/alerts/evaluate` | SERVICE | 30 tokens | 2/sec | ~15 sec to exhaust |

## Setup Steps

### Step 1: Import Environment & Collection

1. **Open Postman**
2. **Import Environment:**
   - Click `Collections` → `Import` → `Upload Files`
   - Select `rate-limit-environment.json`
   - Click `Import`

3. **Import Collection:**
   - Click `Collections` → `Import` → `Upload Files`
   - Select `rate-limit-collection.json`
   - Click `Import`

### Step 2: Configure Environment Variables

1. Click the environment dropdown (top-right)
2. Select **"Rate Limit Testing Environment"**
3. Click the **eye icon** to expand the environment
4. Update these values:

```
base_url                → http://localhost:8080
admin_token             → [Real JWT token with ROLE_ADMIN] 
service_id              → service-001 (or your service ID)
viewer_token            → [Real JWT token with ROLE_VIEWER]
```

#### Getting Real JWT Tokens

**For ADMIN role:**
```bash
# Make a login request to get token
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'
```

**For VIEWER role:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"viewer","password":"password"}'
```

Copy the `token` from response and paste into environment variables.

---

## Testing Strategies

### Test 1: Rapid-Fire Requests to Trigger Rate Limit

**Goal:** Exhaust token bucket quickly

1. Select `POST /api/metrics/ingest (SERVICE)` request
2. Right-click → **"Run using Collection Runner"**
3. Configure runner:
   - **Iterations:** 100
   - **Delay:** 0 ms (no delay = max speed)
   - **Keep variable values:** ✓
4. Click **"Run Rate Limit Testing Collection"**

**Expected Results:**
- First ~60 requests will pass (200/201 status)
- Remaining ~40 requests will show `429 Rate Limit Exceeded`
- Test output shows "✗ RATE LIMITED" when limit hit

---

### Test 2: Slow Requests (Test Token Refill)

**Goal:** Verify tokens refill at configured rate

1. Configure runner:
   - **Iterations:** 1
   - **Delay:** 200 ms

2. Run the same endpoint 15 times
   - First 60 requests will pass
   - If waiting appropriate time, tokens should refill
   - For `SERVICE:POST:/api/metrics/ingest` (5 refill/sec):
     - Every 1 second → 5 tokens refilled
     - After 2 seconds → can make 10 more requests

---

### Test 3: Per-User Rate Limit Isolation

**Goal:** Show separate users get separate token buckets

**Scenario A: ADMIN user**
```
Quick succession (no delay):
- Run 50 times → All pass
- Run 50 more → First ~50 pass, rest hit limit
```

**Scenario B: VIEWER user (different bucket)**
```
After exhausting ADMIN bucket:
- Switch to VIEWER role
- VIEWER still has fresh 100 token bucket
- Can still make requests
```

**Setup:**
1. Run admin requests to near limit
2. Duplicate a request, change `Authorization` header to VIEWER token
3. Run VIEWER version → Should NOT be rate limited

---

### Test 4: Cross-Service Rate Limit Isolation

**Goal:** Verify different services (X-Service-Id headers) have separate buckets

**Setup:**
1. Create 2 requests with same endpoint but different service IDs:

**Request A:**
```
Header: X-Service-Id: service-001
Body: {...metrics...}
```

**Request B:**
```
Header: X-Service-Id: service-002
Body: {...metrics...}
```

2. Run Runner:
   - Create 2 separate runner sessions
   - One with service-001 (50 iterations)
   - One with service-002 (50 iterations)
   - Both should pass independently

---

### Test 5: Non-POST Requests (Should NOT be limited)

**Setup:**
1. Select request: `GET /api/deployments`
2. Run 500 iterations with 0ms delay
3. All requests should pass (never get 429)

---

## Runner Configuration for Each Test

### Quick Test (Stress Test)
```
Collection:    Rate Limit Testing Collection
Environment:   Rate Limit Testing Environment
Iterations:    200
Delay:         0 ms
Request Timeout: 3000 ms
Keep values:   ✓
Save cookies:  ✓
```

### Controlled Test (Token Refill)
```
Collection:    Rate Limit Testing Collection
Environment:   Rate Limit Testing Environment
Iterations:    10
Delay:         1000 ms (= 1 second between requests)
Request Timeout: 5000 ms
Keep values:   ✓
Save cookies:  ✓
```

### Load Test (Sustained)
```
Collection:    Rate Limit Testing Collection
Environment:   Rate Limit Testing Environment
Iterations:    1000
Delay:         100 ms (spread over time)
Request Timeout: 10000 ms
Keep values:   ✓
```

---

## Interpreting Results

### Console Output Example

```
Request 1-60    ✓ ALLOWED (200 OK) 
Request 61      ✓ ALLOWED (200 OK) ← Last token consumed
Request 62      ✗ RATE LIMITED (429 Too Many Requests)
Request 63      ✗ RATE LIMITED (429 Too Many Requests)
...
Request 100     ✗ RATE LIMITED (429 Too Many Requests)
```

### Response Analysis

**Allowed Request:**
```
Status: 200
Time: 145ms
Body: {...success response...}
```

**Rate Limited Request:**
```
Status: 429
Time: 2ms (very fast - rejected at filter level)
Body: "Rate limit exceeded"
Content-Type: text/plain
```

---

## Advanced Testing

### Test 6: Concurrent User Simulation

1. In Collection Runner, set:
   - **Iterations:** 100
   - **Delay:** 50 ms

2. Create multiple runner instances (tabs):
   - Each simulates different user/service
   - Run simultaneously
   - Each should have independent rate limit

### Test 7: Refill Timing Accuracy

Test the millisecond-precision refill logic:

```javascript
// Pre-request script
// Record start time
pm.environment.set("request_time", Date.now());
```

```javascript
// Test script
// Verify refill happens within ±100ms
let diff = Date.now() - pm.environment.get("request_time");
console.log("Request took: " + diff + "ms");
```

---

## Verification Checklist

- [ ] All ADMIN endpoints respect 429 limit
- [ ] All SERVICE endpoints respect 429 limit
- [ ] GET requests NEVER return 429
- [ ] Different users have separate buckets
- [ ] Different service IDs have separate buckets
- [ ] Tokens refill at configured rate (5/sec, 2/sec, etc.)
- [ ] Response includes `Content-Type: text/plain`
- [ ] Response includes `Rate limit exceeded` message
- [ ] HTTP 429 status code returned (not 400/403)

---

## Troubleshooting

### Issue: All requests return 401
**Solution:** JWT token expired or invalid
- Get fresh token from `/auth/login`
- Update environment variable

### Issue: All requests return 404
**Solution:** Endpoint doesn't exist or different port
- Verify `base_url` is correct (e.g., http://localhost:8080)
- Check deployed service is running

### Issue: No 429 errors despite high iteration count
**Solution:** Rate limit not being triggered
- Check token bucket capacity is correct (should see 429 after ~60 requests for 60-capacity endpoint)
- Verify RateLimitFilter is added to SecurityConfig filter chain
- Check X-Service-Id header is present for SERVICE endpoints

### Issue: Rate limiting happens too early
**Solution:** Verify configured capacity matches endpoint
- `/api/metrics/ingest` should be 60 (not 30)
- Check RateLimitConfig bean is loaded

---

## Quick Start Command

```bash
# 1. Start backend
mvn spring-boot:run

# 2. In Postman: Import both JSON files
# 3. Set environment variables (tokens, base_url)
# 4. Open Collection Runner
# 5. Select "Rate Limit Testing Collection"
# 6. Set Iterations: 100, Delay: 0
# 7. Click "Run"
```

**Result:** Watch console for "✓ ALLOWED" then "✗ RATE LIMITED" switching
