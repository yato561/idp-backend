# IDP Backend - Detailed Features Overview

## Overview
This document provides a comprehensive analysis of key architectural features in the IDP Backend system, including RBAC, TBAC, Rate Limiting, Pagination, AOP, and Alert Management.

---

## 1. RBAC (Role-Based Access Control)

### Overview
Role-Based Access Control is implemented using Spring Security with JWT tokens and role-based annotations.

### Files Involved
- [config/SecurityConfig.java](config/SecurityConfig.java)
- [config/JwtUtil.java](config/JwtUtil.java)
- [config/JwtFilter.java](config/JwtFilter.java)
- [service/impl/CustomUserDetailsService.java](service/impl/CustomUserDetailsService.java)
- [service/impl/AuthServiceImpl.java](service/impl/AuthServiceImpl.java)
- [entity/UserEntity.java](entity/UserEntity.java)
- [util/SecurityUtil.java](util/SecurityUtil.java)
- [config/SecurityBootStrap.java](config/SecurityBootStrap.java)

### Key Implementations

#### 1. User Entity with Roles
```java
// UserEntity stores roles as a Set of Strings in a collection table
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "user_roles")
@Column(name="role")
private Set<String> roles;  // e.g., "ADMIN", "VIEWER"
```

#### 2. JWT Token Generation
**File**: [JwtUtil.java](config/JwtUtil.java)
- Generates JWT tokens containing username and roles as claims
- Uses HMAC-SHA encryption with configurable secret
- Includes expiry time configuration

#### 3. JWT Validation Filter
**File**: [JwtFilter.java](config/JwtFilter.java)
- Extends `OncePerRequestFilter` to process JWT on every request
- Extracts Bearer token from Authorization header
- Validates JWT claims and converts roles to Spring Security authorities
- Handles role prefix normalization (adds "ROLE_" if missing)
- Sets authenticated user in SecurityContextHolder

#### 4. Custom UserDetailsService
**File**: [CustomUserDetailsService.java](service/impl/CustomUserDetailsService.java)
- Implements `UserDetailsService` interface
- Loads user from database and constructs UserDetails with roles
- Converts roles to SimpleGrantedAuthority objects

#### 5. Authentication Management
**File**: [AuthServiceImpl.java](service/impl/AuthServiceImpl.java)
- **Register**: Creates new users with roles and team assignment
- **Login**: Authenticates with username/password, returns JWT access token + refresh token
- **Refresh**: Validates refresh token and generates new access token
- **Logout**: Revokes refresh token

#### 6. Role Checking Utilities
**File**: [SecurityUtil.java](util/SecurityUtil.java)
```java
- currentUsername()     // Get authenticated user's name from SecurityContext
- hasRole(String role)  // Check if user has specific role
- isAdmin()             // Check if user is ADMIN
- currentUserTeam()     // Get authenticated user's team
- isAuthenticated()     // Check if user is authenticated
```

### How It Works - Flow Diagram
```
1. User Registration/Login
   RegisterRequest → AuthService → encrypt password → save UserEntity with roles

2. JWT Token Generation
   UserEntity (with roles) → JwtUtil → signed JWT token (contains roles as claims)

3. Request Processing
   Authorization: Bearer {token}
   ↓
   JwtFilter extracts token
   ↓
   JwtUtil.validate() → extracts roles from claims
   ↓
   Convert roles to GrantedAuthority with "ROLE_" prefix
   ↓
   Create UsernamePasswordAuthenticationToken
   ↓
   Set in SecurityContextHolder for request lifecycle

4. Authorization Check
   @PreAuthorize("hasRole('ADMIN')")
   ↓
   Spring Security checks GrantedAuthority
   ↓
   Allow or deny access
```

### Integration Points
- **Controllers**: Use `@PreAuthorize` annotations to enforce role requirements
  - `@PreAuthorize("hasRole('ADMIN')")`
  - `@PreAuthorize("hasAnyRole('ADMIN','VIEWER')")`
- **SecurityConfig**: Configures filter chain with JwtFilter before UsernamePasswordAuthenticationFilter
- **Database**: UserEntity with user_roles collection table
- **JWT Configuration**: `jwt.secret` and `jwt.expiry` properties

---

## 2. TBAC (Team-Based Attribute-Based Access Control)

### Overview
Team-Based Access Control enforces data isolation and modification permissions based on user team membership. Complements RBAC by adding team-based ownership validation.

### Files Involved
- [security/OwnershipGuard.java](security/OwnershipGuard.java)
- [util/SpecUtil.java](util/SpecUtil.java)
- [util/SecurityUtil.java](util/SecurityUtil.java)
- [entity/ServiceCatInfo.java](entity/ServiceCatInfo.java)
- [service/impl/ServiceCatServiceImpl.java](service/impl/ServiceCatServiceImpl.java)
- [util/ServiceSpec.java](util/ServiceSpec.java)

### Key Implementations

#### 1. OwnershipGuard Component
**File**: [OwnershipGuard.java](security/OwnershipGuard.java)
```java
public void assertCanModify(ServiceCatInfo serviceCatInfo) throws AccessDeniedException {
    // Admins can always modify
    if (SecurityUtil.isAdmin()){
        return;
    }

    // Non-admins must be in the same team as the service owner
    String userTeam = normalize(user.getTeam());
    String ownerTeam = normalize(serviceCatInfo.getOwnerTeam());

    if(!userTeam.equals(ownerTeam)){
        throw new AccessDeniedException("Not allowed to modify this service");
    }
}
```

**Features**:
- Normalization: Converts to lowercase and trims whitespace
- Admin bypass: Admins bypass team checks
- Team comparison: Strict equality check on normalized team names

#### 2. Dynamic Specification Builder
**File**: [SpecUtil.java](util/SpecUtil.java)
```java
SpecUtil.<ServiceCatInfo>of()
    .eq("runTime", runtime)          // Exact match filter
    .eq("status", status)             // Chained filters
    .enforceIf(!isAdmin,              // Conditional filter
      "ownerTeam", 
      SecurityUtil.currentUserTeam()) // Enforce team filter if not admin
    .build()
```

**How it works**:
- Builds JPA Specification objects dynamically
- `eq()`: Adds equality predicates
- `enforceIf()`: Conditionally adds predicates based on boolean flag
- `build()`: Combines all predicates with AND logic

#### 3. Data Filtering in Service Layer
**File**: [ServiceCatServiceImpl.java](service/impl/ServiceCatServiceImpl.java#L124)
```java
@Override
public Page<ServiceCatResponse> listServices(String runtime, String status, String ownerTeam, Pageable pageable) {
    boolean isAdmin = SecurityUtil.isAdmin();

    Specification<ServiceCatInfo> spec = SpecUtil.<ServiceCatInfo>of()
        .eq("runTime", runtime)
        .eq("status", status)
        .enforceIf(!isAdmin, "ownerTeam", SecurityUtil.currentUserTeam())
        .build();

    return serviceDao.findAll(spec, pageable)
            .map(ServiceCatMapper::toResponse);
}
```

**Behavior**:
- **ADMIN users**: See all services (no team filter)
- **Non-admin users**: See only services belonging to their team
- **Filtering**: Applied at database level via JPA Specification

### Attribute Mapping
```
UserEntity.team ←→ ServiceCatInfo.ownerTeam

Example:
User: {username: "alice", team: "platform", role: "VIEWER"}
Service: {serviceName: "api-gateway", ownerTeam: "platform"}
→ Alice can view this service

Service: {serviceName: "db-service", ownerTeam: "infra"}
→ Alice CANNOT view this service (different team)
```

### Integration Points
- **Service Update/Delete**: Uses OwnershipGuard.assertCanModify()
- **Service Listing**: Uses SpecUtil with enforceIf() for team-based filtering
- **Authorization**: Audit fields track team membership

---

## 3. Rate Limiting

### Overview
Token Bucket algorithm implementation with role-based limits. Applies per-user/per-service quotas with automatic token refill.

### Files Involved
- [rateLimit/RateLimitFilter.java](rateLimit/RateLimitFilter.java)
- [rateLimit/RateLimitConfig.java](rateLimit/RateLimitConfig.java)
- [rateLimit/TokenBucket.java](rateLimit/TokenBucket.java)
- [rateLimit/RateLimitRegistry.java](rateLimit/RateLimitRegistry.java)
- [rateLimit/RateLimitRule.java](rateLimit/RateLimitRule.java)

### Key Implementations

#### 1. Token Bucket Algorithm
**File**: [TokenBucket.java](rateLimit/TokenBucket.java)
```java
public class TokenBucket {
    private int tokens;           // Current token count
    private final int capacity;   // Max tokens
    private final int refillRate; // Tokens per second
    private long lastRefillMillis; // Last refill time

    synchronized boolean tryConsume(){
        refill();  // Calculate and add tokens since last refill
        if(tokens > 0){
            tokens--;  // Consume one token
            return true;
        }
        return false;  // No tokens available
    }

    private void refill(){
        long elapsedMillis = now - lastRefillMillis;
        long refillTokens = (elapsedMillis * refillRate) / 1000L;
        tokens = Math.min(capacity, tokens + refillTokens);
    }
}
```

**Features**:
- Synchronized for thread safety
- Automatic token refill at configured rate
- Never exceeds capacity
- Millisecond precision for refill calculation

#### 2. Rate Limit Rules Configuration
**File**: [RateLimitConfig.java](rateLimit/RateLimitConfig.java)
```java
Map<String, RateLimitRule> rules = new HashMap<>();

// Format: "ROLE:METHOD:ENDPOINT" → capacity, refill_rate
rules.put("ADMIN:POST:/api/deployments",      new RateLimitRule(100, 10));   // 100 tokens, 10/sec
rules.put("ADMIN:POST:/api/logs/ingest",      new RateLimitRule(200, 20));   // 200 tokens, 20/sec
rules.put("SERVICE:POST:/api/metrics/ingest", new RateLimitRule(60, 5));     // 60 tokens, 5/sec
rules.put("SERVICE:POST:/api/alerts/evaluate", new RateLimitRule(30, 2));    // 30 tokens, 2/sec
```

#### 3. Rate Limit Filter
**File**: [RateLimitFilter.java](rateLimit/RateLimitFilter.java)
```java
@Override
protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain){
    // 1. Only applies to POST requests
    if(!"POST".equalsIgnoreCase(req.getMethod())){
        chain.doFilter(req, res);
        return;
    }

    // 2. Determine user role
    String role;
    if (req.getHeader("X-Service-Id") != null) {
        role = "SERVICE";           // Service-to-service call
    } else if (SecurityUtil.isAdmin()) {
        role = "ADMIN";             // Admin user
    } else {
        role = "VIEWER";            // Regular user
    }

    // 3. Resolve rate limit rule
    String ruleKey = role + ":" + req.getMethod().toUpperCase() + ":" + path;
    RateLimitRule rule = rules.get(ruleKey);
    if (rule == null) {
        chain.doFilter(req, res);  // No rate limit for this endpoint
        return;
    }

    // 4. Resolve bucket key (per-user/per-service isolation)
    String identifier = resolveKey(req);  // username, serviceId, or IP
    String bucketKey = ruleKey + ":" + identifier;

    // 5. Check and consume token
    TokenBucket bucket = registry.getBucket(bucketKey, rule.capacity(), rule.refillRatePerSecond());
    if (!bucket.tryConsume()) {
        res.setStatus(429);                    // Too Many Requests
        res.setContentType("text/plain");
        res.getWriter().write("Rate limit exceeded");
        return;
    }

    chain.doFilter(req, res);  // Request allowed
}

private String resolveKey(HttpServletRequest request) {
    String serviceId = request.getHeader("X-Service-Id");
    if(serviceId != null) return serviceId;

    String user = SecurityUtil.currentUsername();
    if(user != null) return user;

    return request.getRemoteAddr();  // Fallback to IP
}
```

#### 4. Registry for Bucket Management
**File**: [RateLimitRegistry.java](rateLimit/RateLimitRegistry.java)
```java
@Component
public class RateLimitRegistry {
    private final Map<String, TokenBucket> bucket = new ConcurrentHashMap<>();

    public TokenBucket getBucket(String key, int capacity, int refillRate){
        return bucket.computeIfAbsent(key, k -> new TokenBucket(capacity, refillRate));
    }
}
```

### How It Works - Example Flow
```
User: alice (ADMIN)
Endpoint: POST /api/deployments
Rate Limit Rule: "ADMIN:POST:/api/deployments" → 100 tokens, 10/sec

Request 1-100: ALLOWED (consume tokens 100→0)
Request 101: RATE LIMITED (no tokens, returns 429)
Wait 1 second → 10 tokens refill (0→10)
Request 102: ALLOWED (consume token 10→9)
```

### Bucket Key Isolation
```
Different users → Different buckets:
"ADMIN:POST:/api/deployments:alice"     → separate 100-token bucket
"ADMIN:POST:/api/deployments:bob"       → separate 100-token bucket

Different endpoints → Different buckets:
"ADMIN:POST:/api/deployments:alice"     → 100 tokens
"ADMIN:POST:/api/logs/ingest:alice"     → 200 tokens

Different roles → Different buckets:
"ADMIN:POST:/api/deployments:alice"     → 100 tokens, 10/sec
"SERVICE:POST:/api/metrics/ingest:service-001" → 60 tokens, 5/sec
```

### Integration Points
- **SecurityConfig**: RateLimitFilter added to filter chain after JwtFilter
- **Controllers**: Endpoints configured with specific rate limit rules
- **Response**: Returns HTTP 429 with "Rate limit exceeded" message
- **Testing**: Shell and PowerShell scripts in postman/ directory

---

## 4. Pagination

### Overview
Spring Data JPA Pagination integrated throughout the service layer with custom utilities.

### Files Involved
- [util/PaginationUtil.java](util/PaginationUtil.java)
- [repo/MetricsRepo.java](repo/MetricsRepo.java)
- [repo/DeploymentRepo.java](repo/DeploymentRepo.java)
- [repo/LogsRepo.java](repo/LogsRepo.java)
- [dao/impl/LogsDaoImpl.java](dao/impl/LogsDaoImpl.java)
- [service/impl/LogsServiceImpl.java](service/impl/LogsServiceImpl.java)
- [service/impl/ServiceCatServiceImpl.java](service/impl/ServiceCatServiceImpl.java)
- [controller/ServiceCatController.java](controller/ServiceCatController.java)
- [controller/LogsController.java](controller/LogsController.java)

### Key Implementations

#### 1. Pagination Utility
**File**: [PaginationUtil.java](util/PaginationUtil.java)
```java
public static <T, R extends JpaRepository<T,?> & JpaSpecificationExecutor<T>>
    Page<T> paginate(R repo, Specification<T> spec, Pageable pageable) {
    if(spec == null){
        return repo.findAll(pageable);
    }
    return repo.findAll(spec, pageable);
}
```

**Features**:
- Generic utility for any entity type
- Works with or without Specification filters
- Delegates to Spring Data JPA

#### 2. Repository with Pagination Support
**File**: [MetricsRepo.java](repo/MetricsRepo.java)
```java
public interface MetricsRepo extends 
    JpaRepository<MetricEntity, UUID>,
    JpaSpecificationExecutor<MetricEntity> {

    Page<MetricEntity> findByServiceId(UUID serviceId, Pageable pageable);

    @Query("SELECT ... FROM MetricEntity m WHERE m.serviceId = :serviceId ...")
    SummaryProjection summarize(
        @Param("serviceId") UUID serviceId,
        @Param("from") Instant from,
        @Param("to") Instant to
    );
}
```

#### 3. Service Layer Pagination
**File**: [LogsServiceImpl.java](service/impl/LogsServiceImpl.java#L29)
```java
@Override
public Page<LogResponse> getLog(UUID serviceId, Instant from, Instant to, String level, Pageable page) {
    Specification<LogEntity> spec = (root, q, cb) -> cb.equal(root.get("serviceId"), serviceId);

    if (level != null)
        spec = spec.and((r, q, c) -> c.equal(r.get("level"), level));

    if (from != null)
        spec = spec.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("timestamp"), from));

    if (to != null)
        spec = spec.and((r, q, c) -> c.lessThanOrEqualTo(r.get("timestamp"), to));

    return logsDao.search(spec, page).map(LogMapper::toResponse);
}
```

#### 4. DAO Layer Pagination
**File**: [LogsDaoImpl.java](dao/impl/LogsDaoImpl.java)
```java
@Component
public class LogsDaoImpl implements LogsDao {
    @Override
    public Page<LogEntity> search(Specification<LogEntity> spec, Pageable page) {
        return repo.findAll(spec, page);
    }
}
```

#### 5. Controller Endpoint
**File**: [ServiceCatController.java](controller/ServiceCatController.java#L25)
```java
@GetMapping("/all")
public ResponseEntity<Page<ServiceCatResponse>> list(
    @RequestParam(required = false) String runtime,
    @RequestParam(required = false) String status,
    @RequestParam(required = false) String ownerTeam,
    Pageable pageable  // Spring automatically converts query params to Pageable
) {
    return ResponseEntity.ok(service.listServices(runtime, status, ownerTeam, pageable));
}
```

### Request/Response Format
```
GET /api/services/all?page=0&size=20&sort=createdAt,desc&runtime=java&status=ACTIVE

Query Parameters (Spring Data JPA standard):
- page: 0-indexed page number (default: 0)
- size: Page size (default: 20)
- sort: field1,asc|desc;field2,asc|desc

Response:
{
    "content": [
        { serviceId, serviceName, status, ... },
        { ... }
    ],
    "pageable": {
        "pageNumber": 0,
        "pageSize": 20,
        "sort": { ... },
        "offset": 0
    },
    "last": false,
    "totalElements": 150,
    "totalPages": 8,
    "size": 20,
    "number": 0,
    "numberOfElements": 20,
    "first": true,
    "empty": false
}
```

### Integration Points
- **Controllers**: Accept `Pageable` parameter from query strings
- **Services**: Pass Pageable to DAO layer
- **DAOs**: Use repo.findAll(spec, pageable)
- **Repositories**: Extend JpaRepository and JpaSpecificationExecutor

---

## 5. AOP (Aspect-Oriented Programming)

### Overview
Cross-cutting audit logging implemented using Spring AOP with method-level annotations.

### Files Involved
- [audit/Auditable.java](audit/Auditable.java)
- [audit/AuditAspect.java](audit/AuditAspect.java)
- [entity/AuditLog.java](entity/AuditLog.java)
- [repo/AuditLogRepo.java](repo/AuditLogRepo.java)

### Key Implementations

#### 1. Auditable Annotation
**File**: [Auditable.java](audit/Auditable.java)
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String action();                    // e.g., "UPDATE_SERVICE", "DELETE_SERVICE"
    String resource();                  // e.g., "SERVICE", "DEPLOYMENT"
    String resourceIdParam() default ""; // Parameter name containing resource ID
}
```

#### 2. Audit Aspect
**File**: [AuditAspect.java](audit/AuditAspect.java)
```java
@Aspect
@Component
public class AuditAspect {
    
    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void logSuccess(JoinPoint jp, Auditable auditable, Object result) {
        persist(auditable, jp, "SUCCESS", null);
    }

    @AfterThrowing(pointcut = "@annotation(auditable)", throwing = "ex")
    public void logFailure(JoinPoint jp, Auditable auditable, Exception ex) {
        persist(auditable, jp, "FAILURE", ex.getMessage());
    }

    private void persist(Auditable auditable, JoinPoint jp, String outcome, String error) {
        AuditLog log = new AuditLog();
        log.setActor(SecurityUtil.currentUsername());           // WHO
        log.setAction(auditable.action());                      // WHAT action
        log.setResource(auditable.resource());                  // WHAT resource
        log.setOutcome(outcome);                                // SUCCESS/FAILURE
        log.setError(error);                                    // Error message if failed
        log.setTimestamp(Instant.now());                        // WHEN

        // Extract resource ID from method parameters
        if (!auditable.resourceIdParam().isBlank()) {
            String id = extractResourceId(jp, auditable.resourceIdParam());
            log.setResourceId(id);
        }

        repo.save(log);
    }

    private String extractResourceId(JoinPoint jp, String paramName) {
        MethodSignature sig = (MethodSignature) jp.getSignature();
        String[] names = sig.getParameterNames();
        Object[] values = jp.getArgs();

        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(paramName)) {
                return String.valueOf(values[i]);
            }
        }
        return null;
    }
}
```

#### 3. Audit Log Entity
**File**: [AuditLog.java](entity/AuditLog.java)
```java
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue
    private Long id;

    private String actor;         // Username who performed action
    private String action;        // Action name (e.g., UPDATE_SERVICE)
    private String resource;      // Resource type (e.g., SERVICE)
    private String resourceId;    // UUID of the resource
    private String outcome;       // SUCCESS or FAILURE
    private String error;         // Error message if failed
    private Instant timestamp;    // When action occurred
}
```

#### 4. Usage Example
**File**: [ServiceCatServiceImpl.java](service/impl/ServiceCatServiceImpl.java#L65)
```java
@Override
@Auditable(
    action = "UPDATE_SERVICE",
    resource = "SERVICE",
    resourceIdParam = "id"     // Extract 'id' parameter as resource ID
)
public ServiceCatResponse update(UUID id, ServiceCatRequest request) 
    throws AccessDeniedException {
    // Method implementation...
}
```

### How It Works - Flow
```
1. Method with @Auditable is called
   ↓
2. AuditAspect intercepts the call (pointcut matches)
   ↓
3. Method executes
   ↓
4. Two outcomes:
   a) SUCCESS: @AfterReturning advice executes
      → Creates AuditLog with outcome="SUCCESS"
   
   b) FAILURE: @AfterThrowing advice executes
      → Creates AuditLog with outcome="FAILURE" and exception message
   ↓
5. AuditLog saved to database
```

### Audit Trail Example
```
Scenario: User 'alice' updates service 'svc-123'

AuditLog entry:
{
    id: 42,
    actor: "alice",
    action: "UPDATE_SERVICE",
    resource: "SERVICE",
    resourceId: "svc-123",
    outcome: "SUCCESS",
    error: null,
    timestamp: 2025-05-19T10:30:45.123Z
}

Scenario: User 'bob' tries to delete service from another team (fails)

AuditLog entry:
{
    id: 43,
    actor: "bob",
    action: "DELETE_SERVICE",
    resource: "SERVICE",
    resourceId: "svc-456",
    outcome: "FAILURE",
    error: "Only Admin can delete services",
    timestamp: 2025-05-19T10:31:22.456Z
}
```

### Integration Points
- **Service Methods**: Apply @Auditable annotation to methods
- **Advice Types**: @AfterReturning for success, @AfterThrowing for failures
- **Parameter Extraction**: resourceIdParam matches method parameter names
- **Database**: Audit logs persisted in audit_logs table
- **Querying**: AuditLogRepo provides access to audit history

---

## 6. Alert/Notification System

### Overview
Rule-based alert generation system with configurable thresholds. Evaluates service metrics and automatically triggers alerts.

### Files Involved
- [controller/AlertController.java](controller/AlertController.java)
- [service/AlertService.java](service/AlertService.java)
- [service/impl/AlertServiceImpl.java](service/impl/AlertServiceImpl.java)
- [dao/AlertDao.java](dao/AlertDao.java)
- [dao/impl/AlertDaoImpl.java](dao/impl/AlertDaoImpl.java)
- [repo/AlertRepo.java](repo/AlertRepo.java)
- [entity/AlertEntity.java](entity/AlertEntity.java)
- [dto/AlertResponse.java](dto/AlertResponse.java)
- [util/AlertRuleEngine.java](util/AlertRuleEngine.java)
- [mapper/AlertMapper.java](mapper/AlertMapper.java)

### Key Implementations

#### 1. Alert Entity
**File**: [AlertEntity.java](entity/AlertEntity.java)
```java
@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_alert_service_status", columnList = "serviceId,status"),
    @Index(name = "idx_alert_triggered_at", columnList = "triggeredAt")
})
public class AlertEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID serviceId;           // Which service

    @Column(nullable = false)
    private String type;              // CPU_HIGH, MEMORY_HIGH, SERVICE_DOWN

    @Column(nullable = false)
    private String severity;          // HIGH, MEDIUM, LOW

    @Column(nullable = false)
    private String status;            // OPEN, RESOLVED

    @Column(nullable = false)
    private Instant triggeredAt;      // When alert was triggered

    private Instant resolvedAt;       // When alert was resolved

    private String message;           // Alert message
}
```

#### 2. Alert Rule Engine
**File**: [AlertRuleEngine.java](util/AlertRuleEngine.java)
```java
@Component
public class AlertRuleEngine {

    // CPU usage threshold: > 80%
    public boolean cpuHigh(SummaryResponse s) {
        return s.getAvgCpu() != null && s.getAvgCpu() > 80;
    }

    // Memory threshold: > 1024 MB
    public boolean memoryHigh(SummaryResponse s) {
        return s.getAvgMemory() != null && s.getAvgMemory() > 1024;
    }

    // Service status check
    public boolean serviceDown(SummaryResponse s) {
        return "DOWN".equals(s.getStatus());
    }
}
```

#### 3. Alert Service Implementation
**File**: [AlertServiceImpl.java](service/impl/AlertServiceImpl.java)
```java
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final AlertDao dao;
    private final MetricService metricService;
    private final AlertRuleEngine rules;

    @Override
    public void evaluate(UUID serviceId) {
        // 1. Get service metrics summary (15-minute window)
        SummaryResponse s = metricService.getSummaryById(serviceId, 15, null, null);

        if (s == null || "NO_DATA".equals(s.getStatus())) {
            return;  // Can't evaluate without data
        }

        // 2. Check each rule and trigger alerts
        trigger(serviceId, "CPU_HIGH", "MEDIUM", 
            rules.cpuHigh(s),
            "CPU usage exceeded threshold");

        trigger(serviceId, "MEMORY_HIGH", "MEDIUM", 
            rules.memoryHigh(s),
            "Memory usage exceeded threshold");

        trigger(serviceId, "SERVICE_DOWN", "HIGH", 
            rules.serviceDown(s),
            "Service is down");
    }

    private void trigger(UUID serviceId, String type, String severity, 
                        boolean condition, String message) {
        // Skip if condition not met
        if (!condition) return;

        // Avoid duplicate alerts: skip if alert already open for this service/type
        if (dao.hasOpenAlert(serviceId, type)) return;

        // Create and save new alert
        AlertEntity entity = new AlertEntity();
        entity.setServiceId(serviceId);
        entity.setType(type);
        entity.setSeverity(severity);
        entity.setStatus("OPEN");
        entity.setMessage(message);

        dao.save(entity);
    }

    @Override
    public List<AlertResponse> getAll() {
        return dao.findOpen()
            .stream()
            .map(AlertMapper::toResponse)
            .toList();
    }

    @Override
    public List<AlertResponse> getByService(UUID serviceId) {
        return dao.findByService(serviceId)
            .stream()
            .map(AlertMapper::toResponse)
            .toList();
    }

    @Override
    public void resolve(UUID alertId) {
        AlertEntity alert = new AlertEntity();
        alert.setStatus("RESOLVED");
        alert.setResolvedAt(Instant.now());
        dao.save(alert);
    }
}
```

#### 4. Alert Controller
**File**: [AlertController.java](controller/AlertController.java)
```java
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    @Autowired
    private AlertService service;

    // Evaluate metrics and trigger alerts for a service
    @PostMapping("/evaluate/{serviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> evaluate(@PathVariable UUID serviceId) {
        service.evaluate(serviceId);
        return ResponseEntity.ok().build();
    }

    // Get all open alerts
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    public ResponseEntity<List<AlertResponse>> all() {
        return ResponseEntity.ok(service.getAll());
    }

    // Get alerts for specific service
    @GetMapping("/{serviceId}")
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    public ResponseEntity<List<AlertResponse>> byService(@PathVariable UUID serviceId) {
        return ResponseEntity.ok(service.getByService(serviceId));
    }

    // Resolve an alert
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    public ResponseEntity<Void> resolve(@PathVariable UUID id) {
        service.resolve(id);
        return ResponseEntity.ok().build();
    }
}
```

#### 5. Data Access Layer
**File**: [AlertDaoImpl.java](dao/impl/AlertDaoImpl.java)
```java
@Repository
public class AlertDaoImpl implements AlertDao {

    @Autowired
    private AlertRepo repo;

    @Override
    public AlertEntity save(AlertEntity alert) {
        return repo.save(alert);
    }

    @Override
    public List<AlertEntity> findOpen() {
        return repo.findByStatus("OPEN");
    }

    @Override
    public List<AlertEntity> findByService(UUID serviceId) {
        return repo.findByServiceId(serviceId);
    }

    @Override
    public AlertEntity findById(UUID id) {
        return repo.findById(id).orElseThrow();
    }

    @Override
    public boolean hasOpenAlert(UUID serviceId, String type) {
        return repo.existsByServiceIdAndTypeAndStatus(serviceId, type, "OPEN");
    }
}
```

### How It Works - Alert Flow
```
1. External trigger
   POST /api/alerts/evaluate/{serviceId}
   ↓
2. AlertService.evaluate(serviceId) called
   ↓
3. Fetch 15-minute metric summary
   metricService.getSummaryById(serviceId, 15, null, null)
   → Returns: avgCpu, maxCpu, avgMemory, maxMemory, status, etc.
   ↓
4. Evaluate rules via AlertRuleEngine
   - cpuHigh: avgCpu > 80?
   - memoryHigh: avgMemory > 1024?
   - serviceDown: status == "DOWN"?
   ↓
5. For each triggered condition
   a) Check if alert already open for this service/type
   b) If not open, create new AlertEntity
   c) Save to database
   ↓
6. Alert persisted in alerts table
   Indexes on (serviceId, status) for fast queries

7. Query open alerts
   GET /api/alerts
   → Returns all OPEN alerts
   
   GET /api/alerts/{serviceId}
   → Returns alerts for specific service

8. Resolve alert
   POST /api/alerts/{id}/resolve
   → Updates status to "RESOLVED" + sets resolvedAt timestamp
```

### Alert Types & Rules

| Type | Severity | Rule | Threshold |
|------|----------|------|-----------|
| CPU_HIGH | MEDIUM | avgCpu > 80% | 80 |
| MEMORY_HIGH | MEDIUM | avgMemory > 1024MB | 1024 MB |
| SERVICE_DOWN | HIGH | status == DOWN | Service not responding |

### Alert Lifecycle
```
1. OPEN: Alert triggered when condition met
   status: "OPEN"
   triggeredAt: {timestamp}
   resolvedAt: null

2. RESOLVED: Alert marked as resolved
   status: "RESOLVED"
   triggeredAt: {initial timestamp}
   resolvedAt: {current timestamp}

3. Duplicate Prevention: If alert already open for (serviceId, type),
   new alert is not created until first alert is resolved
```

### Integration Points
- **Controllers**: Rate limit on POST /api/alerts/evaluate (30 tokens, 2/sec)
- **Authorization**: @PreAuthorize annotations on endpoints
- **Metrics**: Fetches data from MetricService
- **Database**: Indexed on serviceId, status for fast queries
- **Time Windows**: 15-minute evaluation window (configurable)

---

## Summary Table

| Feature | Pattern | Key Component | How Used |
|---------|---------|----------------|----------|
| **RBAC** | Role-Based Access | SecurityConfig, JwtFilter | @PreAuthorize("hasRole(...)") |
| **TBAC** | Team-Based Access | OwnershipGuard, SpecUtil | assertCanModify(), enforceIf() |
| **Rate Limiting** | Token Bucket | RateLimitFilter, TokenBucket | Request limiting with 429 response |
| **Pagination** | Spring Data JPA | Pageable, PaginationUtil | Pageable parameter in controllers |
| **AOP** | Audit Logging | AuditAspect, @Auditable | Cross-cutting audit trail |
| **Alerts** | Rule Engine | AlertRuleEngine, AlertService | Metric evaluation and triggering |

---

## Security Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     CLIENT REQUEST                              │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  JwtFilter      │
                    │ - Extract token │
                    │ - Validate JWT  │
                    │ - Set Auth      │
                    └────────┬────────┘
                             │
                             ▼
                   ┌──────────────────────┐
                   │ RateLimitFilter      │
                   │ - Check rate limit   │
                   │ - Token bucket check │
                   │ Return 429 if limit  │
                   └─────────┬────────────┘
                             │
                             ▼
                  ┌───────────────────────────┐
                  │ @PreAuthorize Annotation  │
                  │ - Check RBAC role match   │
                  │ - Return 403 if denied    │
                  └──────────┬────────────────┘
                             │
                             ▼
                  ┌───────────────────────────┐
                  │ OwnershipGuard (TBAC)     │
                  │ - Check team membership   │
                  │ - Enforce access policy   │
                  └──────────┬────────────────┘
                             │
                             ▼
                 ┌─────────────────────────────┐
                 │ METHOD EXECUTION            │
                 │ - @Auditable triggers       │
                 │ - AuditAspect logs action   │
                 │ - Returns response          │
                 └────────────┬────────────────┘
                              │
                              ▼
                    ┌──────────────────────┐
                    │  CLIENT RESPONSE     │
                    │  (200/201/403/429)   │
                    └──────────────────────┘
```

