# IDP Backend - Quick Reference Guide

## 🎯 Feature Quick Reference

### 1️⃣ RBAC (Role-Based Access Control)
```
User → Login → JWT Token → Every Request validated → Role checked in @PreAuthorize
↓
ADMIN: Full access to all resources
VIEWER: Read-only, limited write access
```

**Key Files**: `SecurityConfig.java`, `JwtFilter.java`, `SecurityUtil.java`

**Example**:
```java
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> register(...) { }  // Only ADMIN can call

@PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
public ResponseEntity<List> get(...) { }      // Both ADMIN & VIEWER can call
```

---

### 2️⃣ TBAC (Team-Based Access Control)
```
RBAC (Role) + TBAC (Team) = Layered Security
↓
Even with VIEWER role, can only access your team's data
```

**Key Files**: `OwnershipGuard.java`, `SpecUtil.java`

**Example**:
```java
// Non-admin user automatically sees only their team's services
Specification<ServiceCatInfo> spec = SpecUtil.of()
    .enforceIf(!isAdmin(), "ownerTeam", userTeam)  // Only if NOT admin
    .build();
```

**Flow**:
```
User retrieves services
  → OwnershipGuard checks: userTeam == serviceOwnerTeam?
  → If ADMIN: bypass check
  → If not: enforce team match
```

---

### 3️⃣ Rate Limiting
```
Token Bucket Algorithm:
┌─────────────────────┐
│   60 tokens (max)   │
│                     │
│   Refill: 5/sec     │
│                     │
│   Per request:      │
│   1. Refill         │
│   2. Check tokens   │
│   3. If > 0: Allow  │
│   4. Else: 429      │
└─────────────────────┘
```

**Key Files**: `RateLimitFilter.java`, `TokenBucket.java`, `RateLimitConfig.java`

**Rules**:
| Endpoint | Capacity | Refill |
|----------|----------|--------|
| `/api/deployments` | 100 | 50/sec |
| `/api/metrics/ingest` | 20 | 5/sec |
| `/api/logs/ingest` | 30 | 10/sec |

**Response When Limited**: `HTTP 429 Too Many Requests`

---

### 4️⃣ Pagination
```
GET /api/services?page=0&size=20&sort=name,desc
↓
Returns Page with metadata
```

**Key Files**: `PaginationUtil.java`

**Response**:
```json
{
  "content": [...20 items...],
  "totalElements": 150,
  "totalPages": 8,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

---

### 5️⃣ AOP & Audit
```
@Auditable(action="CREATE", resource="SERVICE", resourceIdParam="id")
public ServiceResponse create(UUID id, Request req) {
    // Method logic
}
↓
AuditAspect captures:
  - WHO: actor (username)
  - WHAT: action + resource
  - OUTCOME: SUCCESS/FAILURE
  - WHEN: timestamp
↓
Stored in audit_logs table
```

**Key Files**: `AuditAspect.java`, `Auditable.java`, `AuditLog.java`

**Audit Entry**:
```json
{
  "actor": "john.doe",
  "action": "CREATE_SERVICE",
  "resource": "SERVICE",
  "resourceId": "uuid-123",
  "outcome": "SUCCESS",
  "error": null,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

### 6️⃣ Alerts & Notifications
```
Metrics Collected
  → AlertRuleEngine evaluates:
     • CPU > 80% → CPU_HIGH alert (MEDIUM)
     • Memory > 1024MB → MEMORY_HIGH alert (MEDIUM)
     • Service DOWN → SERVICE_DOWN alert (HIGH)
  → If triggered: Create alert
  → Prevent duplicates
  → Track OPEN → RESOLVED lifecycle
```

**Key Files**: `AlertRuleEngine.java`, `AlertService.java`, `AlertController.java`

**Alert Lifecycle**:
```
POST /api/alerts/evaluate/{serviceId}  → Evaluate rules
GET /api/alerts                         → Get all open alerts
GET /api/alerts/{serviceId}             → Get service's alerts
POST /api/alerts/{id}/resolve           → Mark as resolved
```

---

## 📊 Feature Interaction Matrix

```
Request Flow:

1. Client Request
   ↓
2. JwtFilter (RBAC)
   ↓
3. RateLimitFilter (Rate Limit)
   ↓
4. @PreAuthorize (RBAC)
   ↓
5. OwnershipGuard (TBAC)
   ↓
6. Service Logic + SpecUtil (Pagination + Filtering)
   ↓
7. AuditAspect (Audit Logging)
   ↓
8. Database
```

---

## 🔑 Key Concepts

### RBAC vs TBAC

| Aspect | RBAC | TBAC |
|--------|------|------|
| Level | Coarse (role-level) | Fine (team-level) |
| Example | "Can ADMIN delete?" | "Can delete own team's data?" |
| Enforcement | Annotation | Business logic |
| Use Case | Global permissions | Data isolation |

### Rate Limiting Details

**Token Bucket**: Smooth throttling, handles bursts
```
Capacity: 100 tokens
Refill Rate: 50 tokens/second
Max burst: 100 requests
Sustained rate: 50 requests/second
```

### Audit Trail Benefits

- **Compliance**: Track who changed what when
- **Debugging**: Understand failure root causes
- **Security**: Detect unauthorized access attempts
- **Analytics**: Understand usage patterns

### Alert Evaluation

1. Get last 15 minutes of metrics
2. Calculate averages (CPU, Memory)
3. Check each rule against averages
4. If triggered and no existing OPEN alert: Create
5. Store with timestamps

---

## 🛠️ Common Operations

### Check User Role (in code)
```java
if (SecurityUtil.isAdmin()) { }
if (SecurityUtil.hasRole("VIEWER")) { }
String username = SecurityUtil.currentUsername();
String team = SecurityUtil.currentUserTeam();
```

### Mark Method for Audit
```java
@Auditable(
    action = "UPDATE_SERVICE",
    resource = "SERVICE",
    resourceIdParam = "id"
)
public void update(UUID id, Request req) { }
```

### Build Dynamic Query
```java
Specification<ServiceCatInfo> spec = SpecUtil.of()
    .eq("runtime", runtime)
    .eq("status", status)
    .enforceIf(!isAdmin(), "ownerTeam", userTeam)
    .build();
```

### Trigger Alert Evaluation
```java
// Manual trigger
POST /api/alerts/evaluate/{serviceId}

// Get alerts
GET /api/alerts
GET /api/alerts/{serviceId}

// Resolve
POST /api/alerts/{id}/resolve
```

---

## 📈 Performance Characteristics

| Feature | Time Complexity | Space Complexity | Notes |
|---------|-----------------|------------------|-------|
| RBAC Check | O(1) | O(n roles) | JWT claim extraction |
| TBAC Check | O(1) | O(1) | String comparison |
| Rate Limit | O(1) | O(n users × endpoints) | Bucket lookup |
| Pagination | O(log n) | O(k) | Database index + k items |
| Audit Log | O(1) | O(n) | Single insert |
| Alert Eval | O(m) | O(m) | m = metrics in window |

---

## 🔒 Security Layers

```
Layer 1: Authentication (Who are you?)
         → JWT Token validation
         
Layer 2: Authorization (What can you do?)
         → Role-based checks (@PreAuthorize)
         
Layer 3: Data Isolation (What data can you see?)
         → Team-based filtering (OwnershipGuard, SpecUtil)
         
Layer 4: Throttling (How much can you do?)
         → Rate limiting (Token Bucket)
         
Layer 5: Audit (What did you do?)
         → Audit logging (AOP Aspect)
```

---

## 🎓 Integration Example

### Complete User Journey: Create Service

**1. User calls API**
```bash
POST /api/services
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "serviceName": "payment-service",
  "ownerTeam": "payments-team"
}
```

**2. JwtFilter processes**
- Extract token
- Validate signature & expiration
- Decode roles: ["VIEWER"]
- Set SecurityContext

**3. RateLimitFilter checks**
- Bucket key: "VIEWER:john:/api"
- Refill tokens based on time elapsed
- Tokens remaining: 45 → consume 1 → 44 left
- Allow request

**4. @PreAuthorize validates**
```java
@PostMapping
@PreAuthorize("hasRole('ADMIN')")
// Current user has ["VIEWER"] role
// ❌ AccessDeniedException thrown
```

**5. Response**
```
HTTP 403 Forbidden
{
  "error": "Access Denied",
  "message": "User does not have required role: ADMIN"
}
```

**6. AuditAspect logs failure**
```json
{
  "actor": "john",
  "action": "CREATE_SERVICE",
  "outcome": "FAILURE",
  "error": "User does not have required role: ADMIN",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

### Same Example: Successful (ADMIN User)

**1-2. Same (JwtFilter passes with ADMIN role)**

**3. RateLimitFilter checks**
- Bucket key: "ADMIN:admin:/api"
- Capacity higher for ADMIN
- Allow request

**4. @PreAuthorize validates**
```
User has ["ADMIN"] role ✅
```

**5. OwnershipGuard checks (not needed for CREATE)**

**6. Service creates**
```
Service created with ownerTeam = "payments-team"
```

**7. AuditAspect logs success**
```json
{
  "actor": "admin",
  "action": "CREATE_SERVICE",
  "resource": "SERVICE",
  "resourceId": "550e8400-e29b-41d4-a716-446655440000",
  "outcome": "SUCCESS",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## 📚 File Organization

```
src/main/java/com/idp/backend/
├── config/
│   ├── SecurityConfig.java      ← RBAC config
│   ├── JwtFilter.java           ← Token validation
│   ├── JwtUtil.java             ← Token generation
│   └── HttpClientConfig.java    ← HTTP client
├── security/
│   └── OwnershipGuard.java      ← TBAC enforcement
├── ratelimit/
│   ├── RateLimitFilter.java     ← Rate limit filter
│   ├── TokenBucket.java         ← Token bucket impl
│   ├── RateLimitRegistry.java   ← Bucket registry
│   └── RateLimitConfig.java     ← Rules config
├── audit/
│   ├── AuditAspect.java         ← AOP aspect
│   ├── Auditable.java           ← Audit annotation
│   └── AuditLog.java            ← Audit entity
├── service/
│   ├── AlertService.java        ← Alert interface
│   └── impl/
│       ├── AlertServiceImpl.java ← Alert logic
│       └── ...
├── controller/
│   ├── AlertController.java     ← Alert endpoints
│   └── ...
├── util/
│   ├── SecurityUtil.java        ← Security helpers
│   ├── SpecUtil.java            ← Dynamic queries
│   ├── AlertRuleEngine.java     ← Alert rules
│   ├── PrometheusClient.java    ← Prometheus integration
│   └── PaginationUtil.java      ← Pagination helpers
└── entity/
    ├── AlertEntity.java         ← Alert table
    └── AuditLog.java            ← Audit table
```

---

## 🎯 Next Steps for Development

1. **Extend Alerts**: Add more rules to `AlertRuleEngine`
2. **Custom RBAC**: Define additional roles beyond ADMIN/VIEWER
3. **Notification Gateway**: Integrate alerts with email/Slack
4. **Dashboard**: Create monitoring dashboard with Grafana
5. **Metrics Export**: Use Prometheus for system metrics
6. **API Rate Limit**: Adjust thresholds based on production load

---

**Last Updated**: May 19, 2026  
**Version**: 1.0  
**Status**: Production Ready ✅
