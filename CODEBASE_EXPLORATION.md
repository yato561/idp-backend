# IDP Backend Codebase - Comprehensive Exploration

## Executive Summary
This is a Spring Boot 4.0.1 backend for an Integrated Development Platform (IDP). It manages service catalogs, deployments, metrics, alerts, logs, and user authentication with JWT-based security, role-based access control (RBAC), rate limiting, audit logging, and asynchronous event processing via Kafka.

---

## 1. PROJECT STRUCTURE & TECHNOLOGY STACK

### File Organization
```
src/main/java/com/idp/backend/
├── BackendApplication.java          # Spring Boot entry point
├── controller/                      # REST API endpoints
├── service/                         # Business logic (interfaces & implementations)
├── dao/                            # Data access abstraction layer
├── repo/                           # Spring Data JPA repositories
├── entity/                         # JPA entities
├── dto/                            # Data transfer objects
├── config/                         # Spring Security & JWT configuration
├── security/                       # Custom security components
├── audit/                          # Audit logging via AspectJ
├── ratelimit/                      # Token bucket rate limiting
├── mapper/                         # Entity/DTO mappers
├── util/                           # Utilities (Security, Specification builder, Alert rules)
└── util/async/                     # Async processing (Kafka producers/consumers)
```

### Technology Stack
- **Framework**: Spring Boot 4.0.1
- **Language**: Java 23 (with preview features enabled)
- **Database**: PostgreSQL via Spring Data JPA/Hibernate
- **Security**: Spring Security 6 + JWT (JJWT 0.12.3)
- **Message Broker**: Apache Kafka (spring-kafka)
- **Build Tool**: Maven 3.x
- **Annotations**: Lombok, Jakarta Persistence, AspectJ
- **Monitoring**: Micrometer Prometheus Registry
- **Testing**: Testcontainers, Spring Test Suite

---

## 2. CONTROLLER LAYER - REST API ENDPOINTS

### 2.1 AuthController
**File**: [src/main/java/com/idp/backend/controller/AuthController.java](src/main/java/com/idp/backend/controller/AuthController.java)

| Method | Endpoint | Request | Response | Auth |
|--------|----------|---------|----------|------|
| POST | `/auth/register` | `RegisterRequest` | Void (201 CREATED) | Public |
| POST | `/auth/login` | `LoginRequest` | `AuthResponse` | Public |
| POST | `/auth/refresh` | `RefreshRequest` | `AuthResponse` | Public |
| POST | `/auth/logout` | `LogoutRequest` | Void (204 NO CONTENT) | Public |

**DTOs**:
- `RegisterRequest`: username, password, roles, team
- `LoginRequest`: username, password
- `RefreshRequest`: refreshToken
- `LogoutRequest`: refreshToken
- `AuthResponse`: accessToken, refreshToken

---

### 2.2 ServiceCatController
**File**: [src/main/java/com/idp/backend/controller/ServiceCatController.java](src/main/java/com/idp/backend/controller/ServiceCatController.java)

| Method | Endpoint | Request | Response | Auth |
|--------|----------|---------|----------|------|
| POST | `/api/services` | `ServiceCatRequest` | `ServiceCatResponse` (201 CREATED) | Public |
| GET | `/api/services` | - | `List<ServiceCatResponse>` | Public |
| GET | `/api/services/all` | Pageable + filters | `Page<ServiceCatResponse>` | Public |
| GET | `/api/services/{id}` | - | `ServiceCatResponse` | Public |
| PUT | `/api/services/{id}` | `ServiceCatRequest` | `ServiceCatResponse` | Requires Auth + `@Auditable` |
| DELETE | `/api/services/{id}` | - | Void (204 NO CONTENT) | Requires Auth + `@Auditable` |

**Query Filters** (on /all):
- `runtime`: String (optional)
- `status`: String (optional)
- `ownerTeam`: String (optional)

**Security**: RBAC with team-based enforcement via `OwnershipGuard` (non-ADMIN users can only modify services owned by their team)

---

### 2.3 DeploymentController
**File**: [src/main/java/com/idp/backend/controller/DeploymentController.java](src/main/java/com/idp/backend/controller/DeploymentController.java)

| Method | Endpoint | Request | Response | Auth |
|--------|----------|---------|----------|------|
| POST | `/api/deployments` | `DeploymentRequest` | Void (202 ACCEPTED) | `@PreAuthorize("hasRole('ADMIN')")` + `@Auditable` |
| GET | `/api/deployments/{serviceId}` | Pageable | `Page<DeploymentResponse>` | `@PreAuthorize("hasAnyRole('ADMIN','VIEWER')")` |
| GET | `/api/deployments/{serviceId}/latest` | env (optional) | `DeploymentResponse` | `@PreAuthorize("hasAnyRole('ADMIN','VIEWER')")` |
| GET | `/api/deployments/{serviceId}/summary` | - | `DeploySummaryResponse` | `@PreAuthorize("hasAnyRole('ADMIN','VIEWER')")` |
| POST | `/api/deployments/rollback` | `RollbackRequest` | Void (202 ACCEPTED) | `@PreAuthorize("hasRole('ADMIN')")` + `@Auditable` |

**DTOs**:
- `DeploymentRequest`: serviceId, version, env, status, triggeredBy
- `DeploymentResponse`: deployment details with metadata
- `DeploySummaryResponse`: Map<environment, DeploymentEnvSummary> per service
- `RollbackRequest`: serviceId, env, version, triggeredBy

---

### 2.4 MetricController
**File**: [src/main/java/com/idp/backend/controller/MetricController.java](src/main/java/com/idp/backend/controller/MetricController.java)

| Method | Endpoint | Request | Response | Auth |
|--------|----------|---------|----------|------|
| POST | `/api/metrics/ingest/{serviceId}` | `MetricRequest` | Void (202 ACCEPTED) | `@PreAuthorize("hasRole('ADMIN')")` |
| POST | `/api/metrics/heartbeat/{serviceId}` | - | Void (202 ACCEPTED) | `@PreAuthorize("hasRole('ADMIN')")` |
| GET | `/api/metrics/{serviceId}/latest` | - | `MetricResponse` | `@PreAuthorize("hasAnyRole('ADMIN','VIEWER')")` |
| GET | `/api/metrics/{serviceId}/health` | - | `HealthResponse` | Public |
| GET | `/api/metrics/{serviceId}` | Pageable | `Page<MetricResponse>` | Public |
| GET | `/api/metrics/{serviceId}/summary` | window, from, to (all optional) | `SummaryResponse` | `@PreAuthorize("hasAnyRole('ADMIN', 'VIEWER')")` |

**DTOs**:
- `MetricRequest`: cpuUsage (required), memoryUsageMb, version, deployVersion, env
- `MetricResponse`: id, timestamp, cpuUsage, memoryUsageMb, version, deployVersion, env
- `HealthResponse`: serviceId, lastSeen, healthy (boolean), secondsSinceLastSeen
- `SummaryResponse`: avgCpu, maxCpu, avgMemory, maxMemory, status (HEALTHY/DEGRADED/DOWN), window

---

### 2.5 LogsController
**File**: [src/main/java/com/idp/backend/controller/LogsController.java](src/main/java/com/idp/backend/controller/LogsController.java)

| Method | Endpoint | Request | Response | Auth |
|--------|----------|---------|----------|------|
| POST | `/api/logs/ingest/{serviceId}` | `LogRequest` | Void (202 ACCEPTED) | `@PreAuthorize("hasRole('ADMIN')")` |
| GET | `/api/logs/{serviceId}` | from, to, level (all optional), Pageable | `Page<LogResponse>` | `@PreAuthorize("hasAnyRole('ADMIN','VIEWER')")` |

**DTOs**:
- `LogRequest`: log entries (structure varies)
- `LogResponse`: log details with timestamp, level, message
- **Query Filters**: from (Instant), to (Instant), level (String)

---

### 2.6 AlertController
**File**: [src/main/java/com/idp/backend/controller/AlertController.java](src/main/java/com/idp/backend/controller/AlertController.java)

| Method | Endpoint | Request | Response | Auth |
|--------|----------|---------|----------|------|
| POST | `/api/alerts/evaluate/{serviceId}` | - | Void (200 OK) | `@PreAuthorize("hasRole('ADMIN')")` |
| GET | `/api/alerts` | - | `List<AlertResponse>` | `@PreAuthorize("hasAnyRole('ADMIN','VIEWER')")` |
| GET | `/api/alerts/{serviceId}` | - | `List<AlertResponse>` | `@PreAuthorize("hasAnyRole('ADMIN','VIEWER')")` |
| POST | `/api/alerts/{id}/resolve` | - | Void (200 OK) | `@PreAuthorize("hasAnyRole('ADMIN','VIEWER')")` |

**DTO**: `AlertResponse` - id, serviceId, type, severity, status, triggeredAt, resolvedAt, message

---

## 3. SERVICE LAYER - BUSINESS LOGIC

### 3.1 Service Interfaces & Implementations

#### AuthService
**Interface**: [src/main/java/com/idp/backend/service/AuthService.java](src/main/java/com/idp/backend/service/AuthService.java)
**Implementation**: [src/main/java/com/idp/backend/service/impl/AuthServiceImpl.java](src/main/java/com/idp/backend/service/impl/AuthServiceImpl.java)

**Key Methods**:
- `register(RegisterRequest)`: Creates new user with password encoding via BCryptPasswordEncoder
- `login(LoginRequest)`: Authenticates user, generates JWT + refresh token
- `refresh(RefreshRequest)`: Validates refresh token, generates new JWT
- `logout(LogoutRequest)`: Revokes refresh token
- `assertCanUpdate/assertCanDelete()`: Authorization checks for service modifications

**Dependencies**: `AuthenticationManager`, `UserDao`, `JwtUtil`, `RefreshTokenService`, `PasswordEncoder`

---

#### MetricService
**Interface**: [src/main/java/com/idp/backend/service/MetricService.java](src/main/java/com/idp/backend/service/MetricService.java)
**Implementation**: [src/main/java/com/idp/backend/service/impl/MetricServiceImpl.java](src/main/java/com/idp/backend/service/impl/MetricServiceImpl.java)

**Key Methods**:
- `ingest(UUID, MetricRequest)`: Asynchronously submits metrics via `AsyncIngestor<MetricRequest>` (supports Kafka profile)
- `heartbeat(UUID)`: Records heartbeat for service health monitoring
- `latest(UUID)`: Fetches latest metric (non-null CPU usage)
- `history(UUID, Pageable)`: Paginated metric history
- `getHealth(UUID)`: Calculates health status based on timestamp vs. configured threshold (default 120s)
- `getSummaryById(UUID, window, from, to)`: Aggregates metrics with window support (15-min default), calculates avgCpu, maxCpu, avgMemory, maxMemory, and derives status (HEALTHY/DEGRADED/DOWN)

**Health Status Logic**:
- **HEALTHY**: Last metric ≤ 60s ago
- **DEGRADED**: Last metric 61-180s ago
- **DOWN**: Last metric > 180s ago

**Dependencies**: `MetricsDao`, `ServiceCatDao`, `AsyncIngestor`, `MetricMapper`, Prometheus metrics

---

#### DeploymentService
**Interface**: [src/main/java/com/idp/backend/service/DeploymentService.java](src/main/java/com/idp/backend/service/DeploymentService.java)
**Implementation**: [src/main/java/com/idp/backend/service/impl/DeploymentServiceImpl.java](src/main/java/com/idp/backend/service/impl/DeploymentServiceImpl.java)

**Key Methods**:
- `register(DeploymentRequest)`: Saves deployment record with `@Auditable` annotation
- `history(UUID, Pageable)`: Retrieves paginated deployment history per service
- `latest(UUID, env)`: Fetches latest deployment (with optional environment filter)
- `getSummary(UUID)`: Aggregates deployments by environment into map
- `rollback(RollbackRequest)`: Creates rollback deployment record (status = "ROLLED_BACK")

**Dependencies**: `DeploymentDao`, `DeploymentMapper`

---

#### AlertService
**Interface**: [src/main/java/com/idp/backend/service/AlertService.java](src/main/java/com/idp/backend/service/AlertService.java)
**Implementation**: [src/main/java/com/idp/backend/service/impl/AlertServiceImpl.java](src/main/java/com/idp/backend/service/impl/AlertServiceImpl.java)

**Key Methods**:
- `evaluate(UUID)`: Evaluates metrics against alert rules (CPU_HIGH, MEMORY_HIGH, SERVICE_DOWN), creates new OPEN alerts if conditions triggered
- `getAll()`: Returns all open alerts
- `getByService(UUID)`: Returns alerts for specific service
- `resolve(UUID)`: Marks alert as RESOLVED with timestamp

**Alert Rule Engine** (via `AlertRuleEngine` utility):
- **CPU_HIGH**: avgCpu > 80% (MEDIUM severity)
- **MEMORY_HIGH**: avgMemory > 1024 MB (MEDIUM severity)
- **SERVICE_DOWN**: status == "DOWN" (HIGH severity)

**Dependencies**: `AlertDao`, `MetricService`, `AlertRuleEngine`, `AlertMapper`

---

#### ServiceCatService
**Interface**: [src/main/java/com/idp/backend/service/ServiceCatService.java](src/main/java/com/idp/backend/service/ServiceCatService.java)
**Implementation**: [src/main/java/com/idp/backend/service/impl/ServiceCatServiceImpl.java](src/main/java/com/idp/backend/service/impl/ServiceCatServiceImpl.java)

**Key Methods**:
- `create(ServiceCatRequest)`: Creates service with duplicate name check
- `getAll()`: Returns all services
- `getById(UUID)`: Retrieves single service
- `update(UUID, ServiceCatRequest)`: Updates service with ownership validation via `OwnershipGuard` + `@Auditable`
- `listServices(runtime, status, ownerTeam, Pageable)`: Advanced filtering with RBAC (non-ADMIN users see only their team's services)
- `delete(UUID)`: Deletes service with admin-only check + `@Auditable`

**RBAC Logic**: Built via `SpecUtil` dynamic specification builder:
- **ADMIN**: See all services + optionally filter by ownerTeam
- **Non-ADMIN (VIEWER)**: Enforced filter by `currentUserTeam()` regardless of request parameters

**Dependencies**: `ServiceCatDao`, `AuthService`, `OwnershipGuard`, `SpecUtil`, `SecurityUtil`

---

#### LogsService
**Interface**: [src/main/java/com/idp/backend/service/LogsService.java](src/main/java/com/idp/backend/service/LogsService.java)

**Key Methods**:
- `saveLog(LogRequest, UUID)`: Asynchronous log ingestion (Kafka support)
- `ingestInternal(LogRequest)`: Internal processing for async log consumers
- `getLog(UUID, from, to, level, Pageable)`: Retrieves paginated logs with optional timestamp and level filtering

---

#### RefreshTokenService
**Service for managing refresh token lifecycle** (revocation, creation, validation)

---

## 4. DAO/REPOSITORY LAYER - DATA ACCESS

### 4.1 DAO Pattern Architecture
```
┌─────────────────────────────────────┐
│   Service Layer (Business Logic)    │
└────────────────┬────────────────────┘
                 │ depends on
┌────────────────▼────────────────────┐
│   DAO Interfaces (Abstraction)      │
│   - UserDao                         │
│   - MetricsDao                      │
│   - DeploymentDao                   │
│   - AlertDao                        │
│   - ServiceCatDao                   │
│   - LogsDao                         │
│   - RefreshTokenDao                 │
└────────────────┬────────────────────┘
                 │ implements
┌────────────────▼────────────────────┐
│   DAO Implementations (@Repository) │
│   - UserDaoImpl                      │
│   - MetricsDaoImpl                   │
│   - DeploymentDaoImpl                │
│   - AlertDaoImpl                     │
│   - ServiceDaoImpl                   │
│   - LogsDaoImpl                      │
│   - RefreshTokenDaoImpl              │
└────────────────┬────────────────────┘
                 │ delegates to
┌────────────────▼────────────────────┐
│   Spring Data JPA Repositories      │
│   - UserRepo                        │
│   - MetricsRepo                     │
│   - DeploymentRepo                  │
│   - AlertRepo                       │
│   - ServiceCatRepo                  │
│   - LogsRepo                        │
│   - RefreshTokenRepo                │
└─────────────────────────────────────┘
```

### 4.2 Key DAO Interfaces & Methods

#### UserDao
**Interface**: [src/main/java/com/idp/backend/dao/UserDao.java](src/main/java/com/idp/backend/dao/UserDao.java)

```java
UserEntity save(UserEntity user);
UserEntity findByUsername(String username);  // throws EntityNotFoundException
```

**Implementation**: [src/main/java/com/idp/backend/dao/impl/UserDaoImpl.java](src/main/java/com/idp/backend/dao/impl/UserDaoImpl.java)

---

#### MetricsDao
**Interface**: [src/main/java/com/idp/backend/dao/MetricsDao.java](src/main/java/com/idp/backend/dao/MetricsDao.java)

```java
MetricEntity save(MetricEntity metric);
Page<MetricEntity> findByServiceId(UUID serviceId, Pageable page);
MetricEntity findLatest(UUID serviceId);  // latest non-null CPU usage
MetricEntity fetchHealth(UUID serviceId);  // latest by timestamp
SummaryProjection getSummary(UUID serviceId, Instant from, Instant to);
Instant findLatestTimestamp(UUID serviceId);
```

**Implementation**: [src/main/java/com/idp/backend/dao/impl/MetricsDaoImpl.java](src/main/java/com/idp/backend/dao/impl/MetricsDaoImpl.java)

**Repository**: [src/main/java/com/idp/backend/repo/MetricsRepo.java](src/main/java/com/idp/backend/repo/MetricsRepo.java)

Custom Queries:
```sql
SELECT
    AVG(m.cpuUsage) AS avgCpu,
    MAX(m.cpuUsage) AS maxCpu,
    AVG(m.memoryUsageMb) AS avgMemory,
    MAX(m.memoryUsageMb) AS maxMemory,
    MAX(m.timeStamp) AS lastSeen,
    MAX(m.version) AS version,
    MAX(m.env) AS env
FROM MetricEntity m
WHERE m.serviceId = :serviceId
  AND m.timeStamp BETWEEN :from AND :to
  AND m.cpuUsage IS NOT NULL
  AND m.memoryUsageMb IS NOT NULL
```

---

#### DeploymentDao
**Interface**: [src/main/java/com/idp/backend/dao/DeploymentDao.java](src/main/java/com/idp/backend/dao/DeploymentDao.java)

```java
DeploymentEntity save(DeploymentEntity d);
Page<DeploymentEntity> findByService(UUID serviceId, Pageable page);
DeploymentEntity latestByEnv(UUID serviceId, String env);
DeploymentEntity latest(UUID serviceId);
List<DeploymentEntity> findAllByService(UUID serviceId);
```

---

#### AlertDao
**Interface**: [src/main/java/com/idp/backend/dao/AlertDao.java](src/main/java/com/idp/backend/dao/AlertDao.java)

```java
AlertEntity save(AlertEntity alert);
List<AlertEntity> findOpen();  // status == "OPEN"
List<AlertEntity> findByService(UUID serviceId);
AlertEntity findById(UUID id);
boolean hasOpenAlert(UUID serviceId, String type);  // duplicate prevention
```

---

#### ServiceCatDao
Methods support pagination and JPA Specifications for dynamic filtering:
```java
List<ServiceCatInfo> findAll();
Page<ServiceCatInfo> findAll(Specification<ServiceCatInfo> spec, Pageable pageable);
ServiceCatInfo findById(UUID id);
ServiceCatInfo save(ServiceCatInfo serviceCatInfo);
void delete(ServiceCatInfo serviceCatInfo);
boolean existsByName(String serviceName);
ServiceCatInfo findByServiceName(String name);
```

---

### 4.3 Spring Data JPA Repositories

All DAOs delegate to Spring Data JPA repositories implementing `JpaRepository<T, UUID>` and `JpaSpecificationExecutor<T>`:

**Repositories**:
- [MetricsRepo](src/main/java/com/idp/backend/repo/MetricsRepo.java): Custom JPQL queries for aggregation
- [ServiceCatRepo](src/main/java/com/idp/backend/repo/ServiceCatRepo.java): Dynamic specifications support
- AlertRepo, DeploymentRepo, LogsRepo, UserRepo, RefreshTokenRepo

---

## 5. SECURITY CONFIGURATION

### 5.1 Security Architecture

#### JwtUtil (Token Generation & Validation)
**File**: [src/main/java/com/idp/backend/config/JwtUtil.java](src/main/java/com/idp/backend/config/JwtUtil.java)

```java
public String generateToken(UserEntity user)  // HS256 signed, configurable expiry
public Claims validate(String token)           // Verifies signature & expiration
```

**Claims Structure**:
```json
{
  "sub": "username",
  "roles": ["ROLE_ADMIN", "ROLE_VIEWER"],
  "iat": 1234567890,
  "exp": 1234567890 + jwt.expiry
}
```

**Configuration** (application.properties):
```properties
jwt.secret=very-secret-key-...  # Must be ≥256 bits for HS256
jwt.expiry=900000                # 15 minutes in milliseconds
```

---

#### JwtFilter (Request Authentication)
**File**: [src/main/java/com/idp/backend/config/JwtFilter.java](src/main/java/com/idp/backend/config/JwtFilter.java)

**Flow**:
1. Extracts "Bearer {token}" from Authorization header
2. Validates token via `JwtUtil.validate()`
3. Extracts username and roles from claims
4. Builds Spring Security `UsernamePasswordAuthenticationToken` with authorities
5. Sets in `SecurityContextHolder` for downstream @PreAuthorize checks
6. On failure: Clears security context (not marked as unauthenticated, just cleared)

**Positions in filter chain**:
- Added BEFORE `UsernamePasswordAuthenticationFilter`
- Runs for every request (via `OncePerRequestFilter`)

---

#### SecurityConfig (Filter Chain & Authorization)
**File**: [src/main/java/com/idp/backend/config/SecurityConfig.java](src/main/java/com/idp/backend/config/SecurityConfig.java)

**Security Policies**:
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/auth/**", "/actuator/health")
    .permitAll()                                    // No auth required
    .requestMatchers("/api/services/**")
    .authenticated()                                // Requires JWT
    .anyRequest()
    .authenticated()                                // All other endpoints require auth
)
```

**Filter Chain Order**:
1. CORS disabled (no CORS configuration)
2. Exception handling → Custom `authenticationEntryPoint()` returns 401 JSON
3. JWT Filter (added before default auth filter)
4. Rate Limit Filter (added after JWT filter)

**Password Encoding**: BCryptPasswordEncoder with default strength

**Authentication Entry Point**: Returns JSON:
```json
{
  "error": "Unauthorized",
  "message": "..."
}
```

---

#### SecurityBootStrap (Initialization)
**File**: [src/main/java/com/idp/backend/config/SecurityBootStrap.java](src/main/java/com/idp/backend/config/SecurityBootStrap.java)

Initializes `SecurityUtil.init(userDao)` at application startup for static context access.

---

### 5.2 Authorization Patterns

#### Role-Based Access Control (RBAC)
**Roles Supported**:
- `ROLE_ADMIN`: Full access, can modify any service
- `ROLE_VIEWER`: Read-only access for metrics/deployments/logs

**@PreAuthorize Usage**:
```java
@PreAuthorize("hasRole('ADMIN')")              // Only ADMIN
@PreAuthorize("hasAnyRole('ADMIN','VIEWER')")  // ADMIN or VIEWER
```

#### Team-Based Access Control (TBAC)
**File**: [src/main/java/com/idp/backend/security/OwnershipGuard.java](src/main/java/com/idp/backend/security/OwnershipGuard.java)

```java
public void assertCanModify(ServiceCatInfo serviceCatInfo) throws AccessDeniedException
```

**Logic**:
- ADMINs can modify any service
- Non-ADMINs can only modify services where `serviceCatInfo.ownerTeam == user.team` (case-insensitive)

**Used by**: ServiceCatServiceImpl for update/delete operations

---

### 5.3 SecurityUtil Static Methods
**File**: [src/main/java/com/idp/backend/util/SecurityUtil.java](src/main/java/com/idp/backend/util/SecurityUtil.java)

```java
String currentUsername()                // Get authenticated user
boolean hasRole(String role)            // Check if user has role
String currentUserTeam()                // Get user's team (from DB)
boolean isAdmin()                       // Shortcut for ROLE_ADMIN check
boolean isAuthenticated()               // Check if SecurityContext has valid auth
Specification<ServiceCatInfo> hasRuntime(String runtime)  // Dynamic filter spec
```

---

## 6. RATE LIMITING IMPLEMENTATION

### 6.1 Token Bucket Algorithm
**File**: [src/main/java/com/idp/backend/ratelimit/TokenBucket.java](src/main/java/com/idp/backend/ratelimit/TokenBucket.java)

**Algorithm**:
```java
synchronized boolean tryConsume()
    1. Calculate elapsed time since last refill
    2. Refill tokens: (elapsedMillis * refillRate) / 1000
    3. Cap at capacity
    4. Consume 1 token if available
```

**Parameters**:
- `capacity`: Maximum tokens in bucket (burst allowance)
- `refillRate`: Tokens per second

**Thread-Safe**: Synchronized methods for concurrent request handling

---

### 6.2 Rate Limit Rules Configuration
**File**: [src/main/java/com/idp/backend/ratelimit/RateLimitConfig.java](src/main/java/com/idp/backend/ratelimit/RateLimitConfig.java)

**Rules**:
```java
new RateLimitRule("/api/admin", 100, 50)          // 100 burst, 50 tokens/sec
new RateLimitRule("/api/metrics/ingest", 20, 5)   // 20 burst, 5 tokens/sec
new RateLimitRule("/api/logs/ingest", 30, 10)     // 30 burst, 10 tokens/sec
new RateLimitRule("/api", 60, 20)                  // Default: 60 burst, 20 tokens/sec
```

**Matching**: Longest path prefix wins (most specific rule applies)

---

### 6.3 Rate Limit Filter
**File**: [src/main/java/com/idp/backend/ratelimit/RateLimitFilter.java](src/main/java/com/idp/backend/ratelimit/RateLimitFilter.java)

**Execution**:
1. Matches request path to rate limit rule
2. Only applies to authenticated users (checks `SecurityUtil.isAuthenticated()`)
3. Builds bucket key: `ROLE + ":" + username + ":" + pathPrefix`
4. Resolves/creates bucket from registry
5. Returns **429 (Too Many Requests)** if bucket exhausted
6. Otherwise continues filter chain

**Per-User Rate Limiting**: Each user gets separate buckets per path prefix and role

**Positions in Filter Chain**: Added AFTER JwtFilter → Authenticated users only

---

### 6.4 Rate Limit Registry
**File**: [src/main/java/com/idp/backend/ratelimit/RateLimitRegistry.java](src/main/java/com/idp/backend/ratelimit/RateLimitRegistry.java)

```java
Map<String, TokenBucket> bucket = new ConcurrentHashMap<>()
TokenBucket resolveBucket(String key, RateLimitRule rule)  // computeIfAbsent pattern
```

**Lifecycle**: Buckets created on first request, persisted for session lifetime

---

## 7. AUDIT & LOGGING MECHANISMS

### 7.1 AspectJ-Based Audit Logging
**File**: [src/main/java/com/idp/backend/audit/AuditAspect.java](src/main/java/com/idp/backend/audit/AuditAspect.java)

**Annotation**: [src/main/java/com/idp/backend/audit/Auditable.java](src/main/java/com/idp/backend/audit/Auditable.java)

```java
@Auditable(action="DEPLOY", resource="SERVICE", resourceIdParam="serviceId")
public void register(DeploymentRequest request, @PathVariable UUID serviceId) { ... }
```

**Pointcut**: `@annotation(auditable)` on method execution

**Logged Events**:
- **On Success** (@AfterReturning): Persists AuditLog with outcome = "SUCCESS"
- **On Failure** (@AfterThrowing): Persists AuditLog with outcome = "FAILURE" + exception message

**AuditLog Stored**:
```java
actor (username)
action (from @Auditable.action)
resource (from @Auditable.resource)
resourceId (extracted from method parameter if @Auditable.resourceIdParam specified)
outcome ("SUCCESS" or "FAILURE")
error (exception message, null on success)
timestamp (Instant.now())
```

**Repository**: Persisted via `AuditLogRepo` (Spring Data JPA)

**Used Methods** (with @Auditable):
- `DeploymentServiceImpl.register()`
- `ServiceCatServiceImpl.update()`
- `ServiceCatServiceImpl.delete()`

---

### 7.2 Logging Configuration
**File**: [application.properties](src/main/resources/application.properties)

```properties
logging.level.com.idp.backend=INFO
logging.level.org.springframework.http.converter.json=DEBUG
logging.level.com.fasterxml.jackson=DEBUG
```

**Logger**: Via Lombok `@Slf4j` annotation (SLF4J with Logback backend)

**Example Usage**:
```java
@Slf4j
public class MetricServiceImpl {
    public void getSummaryById(...) {
        log.info("[SUMMARY] serviceId={}, from={}, to={}", serviceId, effectiveFrom, effectiveTo);
    }
}
```

---

## 8. EXCEPTION HANDLING STRATEGY

### 8.1 Global Exception Handler
**File**: [src/main/java/com/idp/backend/exception/GlobalExceptionHandler.java](src/main/java/com/idp/backend/exception/GlobalExceptionHandler.java)

**@RestControllerAdvice**: Centralized exception handling across all controllers

**Exception Mappings**:

| Exception | HTTP Status | Response |
|-----------|-------------|----------|
| `DataIntegrityViolationException` | 400 BAD REQUEST | "Invalid request data" |
| `MethodArgumentNotValidException` | 400 BAD REQUEST | Map<field, error message> |
| `EntityNotFoundException` | 404 NOT FOUND | Exception message |
| `BadCredentialsException` | 401 UNAUTHORIZED | "Invalid credentials" |
| `IllegalArgumentException` | 400 BAD REQUEST | Exception message |
| `TokenException` | 401 UNAUTHORIZED | Exception message |

**Validation Error Format**:
```json
{
  "serviceName": "Service name is required",
  "status": "must not be blank"
}
```

---

### 8.2 Custom Exception: TokenException
**File**: [src/main/java/com/idp/backend/exception/TokenException.java](src/main/java/com/idp/backend/exception/TokenException.java)

Custom runtime exception for JWT-related errors:
```java
public class TokenException extends RuntimeException {
    public TokenException(String message);
    public TokenException(String message, Throwable cause);
}
```

---

## 9. ENTITY/DTO MAPPING LAYER

### 9.1 Mapper Utilities (No ORM Framework)
Manual mapping via static methods:

#### MetricMapper
**File**: [src/main/java/com/idp/backend/mapper/MetricMapper.java](src/main/java/com/idp/backend/mapper/MetricMapper.java)

```java
static MetricEntity toEntity(MetricRequest request, ServiceCatInfo service)
    // Maps DTO to entity, sets serviceId from service object

static MetricEntity heartbeat(UUID serviceId)
    // Creates minimal heartbeat entity (timestamp only)

static MetricResponse toResponse(MetricEntity entity)
    // Entity → DTO for API response
```

**Other Mappers**: AlertMapper, DeploymentMapper, LogMapper, ServiceCatMapper (similar patterns)

---

### 9.2 Key Entities

#### UserEntity
**Table**: `users`
```java
UUID id (PK)
String username (UNIQUE, NOT NULL)
String password (NOT NULL) - BCrypt encrypted
Set<String> roles (@ElementCollection) - Stored in `user_roles` table
String team (NOT NULL) - Team assignment for TBAC
```

#### ServiceCatInfo
**Table**: `services`
```java
UUID serviceId (PK)
String serviceName (UNIQUE, NOT NULL)
String repoUrl
String ownerTeam - Used for team-based access control
String runTime
String status
LocalDateTime createdAt
```

#### MetricEntity
**Table**: `service_metrics`
```java
UUID id (PK)
UUID serviceId (NOT NULL, FK implicit)
Instant timeStamp (NOT NULL)
Double cpuUsage - Nullable for heartbeat-only records
Long memoryUsageMb
String version
String deployVersion
String env
Instant receivedAt (default: Instant.now())
```

#### DeploymentEntity
```java
UUID id
UUID serviceId
String version
String env
String status ("DEPLOYED", "ROLLED_BACK", etc.)
LocalDateTime deployedAt
String triggeredBy
```

#### AlertEntity
```java
UUID id
UUID serviceId
String type ("CPU_HIGH", "MEMORY_HIGH", "SERVICE_DOWN")
String severity ("LOW", "MEDIUM", "HIGH")
String status ("OPEN", "RESOLVED")
Instant triggeredAt
Instant resolvedAt
String message
```

#### LogEntity
```java
UUID id
UUID serviceId
Instant timestamp
String level (INFO, WARN, ERROR, etc.)
String message
String source
```

---

## 10. ASYNCHRONOUS PROCESSING (KAFKA)

### 10.1 Async Ingestor Pattern
**Interface**: [src/main/java/com/idp/backend/util/async/AsyncIngestor.java](src/main/java/com/idp/backend/util/async/AsyncIngestor.java)

```java
public interface AsyncIngestor<T> {
    void submit(T payload);
}
```

**Implementations**:
- **Kafka Profile Active**: `KafkaMetricProducer`, `KafkaLogProducer`
- **Kafka Profile Inactive**: Could have synchronous fallback (not shown)

---

### 10.2 Kafka Producer
**Files**:
- [KafkaMetricProducer.java](src/main/java/com/idp/backend/util/async/KafkaMetricProducer.java)
- [KafkaLogProducer.java](src/main/java/com/idp/backend/util/async/KafkaLogProducer.java) (if exists)

**MetricProducer**:
```java
@Component
@Profile("kafka")
public class KafkaMetricProducer implements AsyncIngestor<MetricRequest> {
    @Override
    public void submit(MetricRequest payload) {
        kafkaTemplate.send("metric-topic", payload.getServiceName(), payload);
    }
}
```

**Topic**: `metric-topic` (partitioned by serviceName)

---

### 10.3 Kafka Consumer Configuration
**Files**:
- [KafkaMetricConsumer.java](src/main/java/com/idp/backend/util/async/KafkaMetricConsumer.java)
- [KafkaLogConsumer.java](src/main/java/com/idp/backend/util/async/KafkaLogConsumer.java)
- [KafkaConfig.java](src/main/java/com/idp/backend/util/async/KafkaConfig.java)

**Pattern**: Consumers call internal service methods (e.g., `MetricService.ingestInternal()`, `LogsService.ingestInternal()`) to persist data

---

## 11. UTILITY COMPONENTS

### 11.1 Dynamic Specification Builder
**File**: [src/main/java/com/idp/backend/util/SpecUtil.java](src/main/java/com/idp/backend/util/SpecUtil.java)

**Builder Pattern for JPA Specifications**:
```java
Specification<ServiceCatInfo> spec = SpecUtil.<ServiceCatInfo>of()
    .eq("runTime", runtime)
    .eq("status", status)
    .enforceIf(!isAdmin, "ownerTeam", SecurityUtil.currentUserTeam())
    .build();

serviceDao.findAll(spec, pageable);
```

**Methods**:
- `eq(field, value)`: Equality filter (null-safe)
- `enforceIf(condition, field, value)`: Conditional filter
- `build()`: Returns composed Specification

---

### 11.2 Alert Rule Engine
**File**: [src/main/java/com/idp/backend/util/AlertRuleEngine.java](src/main/java/com/idp/backend/util/AlertRuleEngine.java)

```java
boolean cpuHigh(SummaryResponse s)      // avgCpu > 80
boolean memoryHigh(SummaryResponse s)   // avgMemory > 1024
boolean serviceDown(SummaryResponse s)  // status == "DOWN"
```

---

## 12. OVERALL ARCHITECTURE PATTERNS

### 12.1 Layered Architecture
```
┌──────────────────────────────────────────────────┐
│          Controllers (REST API)                   │
│  - Handle HTTP requests/responses                 │
│  - Parameter validation via @Valid               │
│  - Authorization via @PreAuthorize               │
└──────────┬───────────────────────────────────────┘
           │
┌──────────▼───────────────────────────────────────┐
│          Services (Business Logic)                │
│  - Implement domain workflows                     │
│  - Decorated with @Auditable for logging         │
│  - Use DAOs for data access                       │
└──────────┬───────────────────────────────────────┘
           │
┌──────────▼───────────────────────────────────────┐
│       DAOs/Repositories (Data Access)             │
│  - Abstract DB queries                            │
│  - Use Spring Data JPA                            │
│  - Support dynamic specifications                 │
└──────────┬───────────────────────────────────────┘
           │
┌──────────▼───────────────────────────────────────┐
│          Database (PostgreSQL)                    │
│  - Persists all entities                          │
│  - Managed by Hibernate ORM                       │
└──────────────────────────────────────────────────┘
```

### 12.2 Cross-Cutting Concerns

**Implemented via**:
- **Security**: Filters + @PreAuthorize + Custom guards
- **Audit Logging**: AspectJ @Auditable
- **Rate Limiting**: Servlet Filter
- **Validation**: JSR-303 @NotNull, @NotBlank, @Valid
- **Exception Handling**: @RestControllerAdvice
- **Async Processing**: Kafka producers/consumers via AsyncIngestor pattern

### 12.3 Request Flow Example: Deploy Service

```
1. POST /api/deployments (with DeploymentRequest)
   ↓
2. SecurityBootStrap initialized JwtFilter
   → Extracts JWT from Authorization header
   → Validates signature & expiration
   → Sets SecurityContextHolder with user + roles
   ↓
3. RateLimitFilter
   → Checks bucket for ROLE_ADMIN:username:/api/deployments
   → Returns 429 if exceeded, else continues
   ↓
4. DeploymentController.register(@PreAuthorize("hasRole('ADMIN')"))
   → Spring Security checks @PreAuthorize (user must have ROLE_ADMIN)
   → Delegates to DeploymentService.register()
   ↓
5. DeploymentService.register(@Auditable)
   → AuditAspect intercepts @Auditable annotation
   → Calls dao.save()
   ↓
6. DeploymentDaoImpl.save()
   → Delegates to DeploymentRepo.save() (Spring Data JPA)
   ↓
7. Hibernate ORM
   → Generates INSERT SQL
   → Executes against PostgreSQL
   ↓
8. Response returned (202 ACCEPTED)
   ↓
9. AuditAspect @AfterReturning
   → Persists AuditLog: actor=username, action=DEPLOY, outcome=SUCCESS
```

### 12.4 RBAC Flow Example: List Services

```
1. GET /api/services/all?runtime=java&status=active
   ↓
2. User authenticated (JWT validated)
   ↓
3. ServiceCatController.listServices(runtime, status, ownerTeam, pageable)
   → No @PreAuthorize = authenticated users only
   ↓
4. ServiceCatServiceImpl.listServices()
   → Check SecurityUtil.isAdmin()
   ↓
5. Dynamic Specification via SpecUtil:
   - If ADMIN: Apply runtime + status filters (ignoreownerTeam param)
   - If VIEWER: Apply runtime + status + enforced ownerTeam==currentUserTeam
   ↓
6. ServiceCatRepo.findAll(spec, pageable)
   → Generates filtered SQL query
   ↓
7. Results mapped to ServiceCatResponse DTOs
   ↓
8. Page<ServiceCatResponse> returned with filtered data only
```

### 12.5 Rate Limiting Scenario

```
User attempts rapid metric ingestion:
1. POST /api/metrics/ingest/service-123 (request 1)
   → Rate limit rule: 20 tokens, 5 tokens/sec
   → Bucket key: ROLE_ADMIN:user123:/api/metrics/ingest
   → New bucket created with 20 tokens
   → Consumes 1 token → 19 remaining ✓
   ↓
2. POST /api/metrics/ingest/service-123 (request 2, 50ms later)
   → Same bucket key
   → Refilled: (50ms * 5 tokens/sec) / 1000ms = 0.25 tokens ≈ 0
   → Total: 19 tokens (no refill yet)
   → Consumes 1 token → 18 remaining ✓
   ↓
3. POST /api/metrics/ingest/service-123 (request 3-20, rapid fire)
   → Tokens consumed until bucket empty
   ↓
4. POST /api/metrics/ingest/service-123 (request 21, immediate)
   → No refill period elapsed
   → 0 tokens available
   → Response: 429 Too Many Requests
```

---

## 13. DATA MODELS & RELATIONSHIPS

### 13.1 Entity Relationships
```
UserEntity (users)
├── Has many roles (user_roles) [Many-to-Many via @ElementCollection]
└── Team → Used for ServiceCatInfo ownership check

ServiceCatInfo (services)
├── Owns many DeploymentEntity (implicit FK on serviceId)
├── Owns many MetricEntity (implicit FK on serviceId)
├── Owns many LogEntity (implicit FK on serviceId)
└── Owns many AlertEntity (implicit FK on serviceId)

MetricEntity (service_metrics)
└── Used by AlertRuleEngine to trigger AlertEntity creation

DeploymentEntity
└── Tracks deployment versions per environment

AlertEntity
├── Created by AlertService.evaluate() based on metrics
└── Can be resolved by users

AuditLog
└── Captures all @Auditable method invocations

RefreshToken
├── Issued during login
└── Validated during refresh token request
```

---

## 14. CONFIGURATION & DEPLOYMENT

### 14.1 Application Configuration
**File**: [application.properties](src/main/resources/application.properties)

```properties
# Server
spring.application.name=backend
server.port=8081

# Actuator (Health checks, metrics)
management.endpoints.web.exposure.include=health
management.endpoints.web.base-path=/actuator
management.endpoint.health.show-details=always
management.endpoint.health.probes.enabled=true

# Database (PostgreSQL)
spring.datasource.url=jdbc:postgresql://localhost:5432/idp
spring.datasource.username=postgres
spring.datasource.password=yato@5531
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.open-in-view=false

# JWT
jwt.secret=very-secret-key-...
jwt.expiry=900000  # 15 minutes

# Logging
logging.level.com.idp.backend=INFO
```

### 14.2 Maven Dependencies
Key dependencies in [pom.xml](pom.xml):
- spring-boot-starter-security
- spring-boot-starter-data-jpa
- spring-boot-starter-web
- spring-kafka
- jjwt (JWT library)
- postgresql (JDBC driver)
- junit, testcontainers (testing)
- lombok (code generation)

### 14.3 Build & Run

```bash
# Build
mvn clean package

# Run
java -jar target/backend-0.0.1-SNAPSHOT.jar

# Or with Maven
mvn spring-boot:run
```

---

## 15. KEY FILES SUMMARY

| Component | Files |
|-----------|-------|
| **Controllers** | AlertController, AuthController, DeploymentController, LogsController, MetricController, ServiceCatController |
| **Services** | AuthService(Impl), MetricService(Impl), DeploymentService(Impl), AlertService(Impl), ServiceCatService(Impl), LogsService, RefreshTokenService(Impl) |
| **DAOs** | UserDao, MetricsDao, DeploymentDao, AlertDao, ServiceCatDao, LogsDao, RefreshTokenDao (+ impl classes) |
| **Repositories** | MetricsRepo, ServiceCatRepo, UserRepo, AlertRepo, DeploymentRepo, LogsRepo, RefreshTokenRepo |
| **Security** | JwtUtil, JwtFilter, SecurityConfig, SecurityBootStrap, OwnershipGuard |
| **Rate Limiting** | RateLimitFilter, TokenBucket, RateLimitRegistry, RateLimitConfig, RateLimitRule |
| **Audit** | AuditAspect, Auditable annotation, AuditLog entity |
| **Exception Handling** | GlobalExceptionHandler, TokenException |
| **Async** | AsyncIngestor, KafkaMetricProducer, KafkaLogProducer, KafkaConfig |
| **Utilities** | SecurityUtil, SpecUtil, AlertRuleEngine, Mappers |
| **Configuration** | application.properties, pom.xml, BackendApplication |

---

## 16. SECURITY CHECKLIST

✅ **Implemented**:
- JWT-based stateless authentication
- Role-based access control (RBAC)
- Team-based access control (TBAC) via OwnershipGuard
- Password encryption (BCrypt)
- Rate limiting (Token Bucket algorithm)
- Audit logging (AspectJ @Auditable)
- CSRF protection disabled (for REST API, appropriate for stateless design)
- Refresh token mechanism with revocation

⚠️ **Production Considerations**:
- JWT secret should be externalized (use environment variables)
- HTTPS enforcement via reverse proxy
- Database credentials in environment variables
- Add request logging for audit trail
- Consider database-backed rate limiting for distributed systems
- Add CORS configuration if frontend on different domain
- Implement refresh token rotation
- Add logout mechanism for token revocation

---

## 17. TESTING INFRASTRUCTURE

**Test Dependencies**:
- Testcontainers + PostgreSQL container (integration tests)
- Spring Test Suite (@SpringBootTest)
- Spring Security Test

**Test Configuration**: [TestcontainersConfiguration.java](src/test/java/com/idp/backend/TestcontainersConfiguration.java), [TestBackendApplication.java](src/test/java/com/idp/backend/TestBackendApplication.java)

---

## CONCLUSION

The IDP backend demonstrates a well-structured Spring Boot microservice with:
- **Layered architecture** (Controllers → Services → DAOs → Database)
- **Security-first design** (JWT + RBAC + TBAC + Rate Limiting)
- **Comprehensive audit trail** (AspectJ-based @Auditable)
- **Scalable async processing** (Kafka integration via AsyncIngestor pattern)
- **Flexible data access** (DAO pattern + dynamic JPA Specifications)
- **Centralized error handling** (GlobalExceptionHandler)

This codebase is suitable for managing microservice deployments, monitoring metrics, managing alerts, and providing secure multi-tenant capabilities with team-based access control.
