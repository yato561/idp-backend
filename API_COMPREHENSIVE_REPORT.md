# Backend APIs - Comprehensive Analysis Report

**Generated:** April 25, 2026  
**Project:** IDP Backend Service  
**Framework:** Spring Boot 4.0.1 | Java 23 | PostgreSQL  
**Architecture Pattern:** Layered Architecture with Microservice Design

---

## Table of Contents
1. [Executive Summary](#executive-summary)
2. [System Architecture Overview](#system-architecture-overview)
3. [Authentication & Security](#authentication--security)
4. [API Endpoints - Detailed Analysis](#api-endpoints---detailed-analysis)
5. [Exception Handling Strategy](#exception-handling-strategy)
6. [Rate Limiting Implementation](#rate-limiting-implementation)
7. [Audit & Logging System](#audit--logging-system)
8. [Edge Cases & Validation](#edge-cases--validation)
9. [Data Models](#data-models)
10. [Async Processing Architecture](#async-processing-architecture)

---

## Executive Summary

### API Statistics
- **Total REST Controllers:** 6
- **Total API Endpoints:** 21
- **Security Model:** JWT-based + RBAC + TBAC
- **Rate Limiting:** Token Bucket Algorithm
- **Audit Trail:** Aspect-oriented logging for all mutations

### Controllers Overview

| Controller | Endpoints | Purpose | Security |
|-----------|-----------|---------|----------|
| **AuthController** | 4 | User authentication, JWT management | Public/Internal |
| **ServiceCatController** | 6 | Service catalog CRUD operations | RBAC + TBAC |
| **DeploymentController** | 5 | Deployment tracking & history | RBAC |
| **MetricController** | 6 | Metrics ingestion & health monitoring | RBAC |
| **LogsController** | 2 | Application logs ingestion & retrieval | RBAC |
| **AlertController** | 4 | Alert evaluation & management | RBAC |

---

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│         Client Application / External Systems          │
└────────────────────┬────────────────────────────────────┘
                     │ HTTPS
                     ▼
┌─────────────────────────────────────────────────────────┐
│     Spring Boot REST API (Port 8080)                    │
├─────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────┐  │
│  │  Filters (RateLimitFilter → JwtFilter)           │  │
│  └──────────────────────────────────────────────────┘  │
│                     │                                   │
│  ┌──────────────────▼──────────────────────────────┐  │
│  │  Controllers                                    │  │
│  │  ├─ AuthController                             │  │
│  │  ├─ ServiceCatController                       │  │
│  │  ├─ DeploymentController                       │  │
│  │  ├─ MetricController                           │  │
│  │  ├─ LogsController                             │  │
│  │  └─ AlertController                            │  │
│  └──────────────────┬───────────────────────────────┘  │
│                     │                                   │
│  ┌──────────────────▼──────────────────────────────┐  │
│  │  @Auditable Aspect → AuditAspect               │  │
│  └──────────────────┬───────────────────────────────┘  │
│                     │                                   │
│  ┌──────────────────▼──────────────────────────────┐  │
│  │  Services (Business Logic)                      │  │
│  │  ├─ AuthService                                │  │
│  │  ├─ ServiceCatService                          │  │
│  │  ├─ DeploymentService                          │  │
│  │  ├─ MetricService                              │  │
│  │  ├─ LogsService                                │  │
│  │  ├─ AlertService                               │  │
│  │  └─ RefreshTokenService                        │  │
│  └──────────────────┬───────────────────────────────┘  │
│                     │                                   │
│  ┌──────────────────▼──────────────────────────────┐  │
│  │  DAO Layer (Data Access)                        │  │
│  │  ├─ Spring Data JPA Repositories               │  │
│  │  └─ Custom DAO Implementations                 │  │
│  └──────────────────┬───────────────────────────────┘  │
└─────────────────────┼───────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
        ▼             ▼             ▼
   ┌─────────┐  ┌──────────┐  ┌──────────┐
   │PostgreSQL│ │Kafka     │  │Audit Log │
   │Database  │ │(Optional)│  │Database  │
   └─────────┘  └──────────┘  └──────────┘
```

---

## Authentication & Security

### 1. JWT (JSON Web Token) Authentication

**Location:** `JwtUtil.java` | `JwtFilter.java`

#### Token Generation
```
Flow: User Registration/Login → JwtUtil.generateToken() 
      → JWT(HS256) with claims + signature
```

**Implementation Details:**
- **Algorithm:** HMAC-SHA256 (HS256)
- **Key:** Externalized in `jwt.secret` property
- **Token Expiry:** Configurable via `jwt.expiry` property (default: 15 minutes)
- **Claims Included:**
  - `sub` (subject): Username
  - `roles`: User roles (ADMIN, VIEWER)
  - `iat` (issued at): Token creation timestamp
  - `exp` (expiration): Token expiration timestamp

**Code Flow:**
```java
// Token Generation (AuthServiceImpl.login())
UserEntity user = userDao.findByUsername(request.getUsername());
String token = jwtUtil.generateToken(user); // Signs with HMAC-SHA256

// Token Validation (JwtFilter)
Claims claims = jwtUtil.validate(token); // Verifies signature
SecurityContextHolder.getContext().setAuthentication(auth);
```

#### Request Authentication
**Header Format:** `Authorization: Bearer <JWT_TOKEN>`

**Validation Flow:**
1. Extract JWT from Authorization header
2. Verify signature using secret key
3. Extract claims (subject, roles)
4. Convert roles to Spring authorities
5. Create SecurityContext with credentials

**Edge Cases Handled:**
- Missing Authorization header → No authentication (public endpoints only)
- Invalid JWT signature → Clear SecurityContext, log error, continue
- Expired token → JwtException caught, context cleared
- Malformed token → Exception handled, request continues unauthenticated

### 2. Role-Based Access Control (RBAC)

**Implementation:** `@PreAuthorize` annotations on controller methods

**Supported Roles:**
- **ADMIN:** Full access to sensitive operations
- **VIEWER:** Read-only access to certain resources

**Examples:**
```java
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> register(DeploymentRequest req) { ... }

@PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
public ResponseEntity<Page<DeploymentResponse>> history(...) { ... }
```

### 3. Team-Based Access Control (TBAC)

**Location:** `OwnershipGuard.java` | `ServiceCatServiceImpl.java`

**Purpose:** Non-admin users can only access/modify services owned by their team

**Implementation:**
```java
public void assertCanModify(ServiceCatInfo serviceCatInfo) throws AccessDeniedException {
    if (SecurityUtil.isAdmin()) return; // Admins bypass check
    
    String userTeam = normalize(user.getTeam());
    String ownerTeam = normalize(serviceCatInfo.getOwnerTeam());
    
    if (!userTeam.equals(ownerTeam)) {
        throw new AccessDeniedException("Permission denied");
    }
}
```

**Where Applied:**
- Service Update: `/api/services/{id}` (PUT)
- Service Delete: `/api/services/{id}` (DELETE)

**Edge Cases:**
- Case-insensitive team comparison (normalized)
- Null team handling (treated as empty string)
- Admin bypass always active

### 4. Refresh Token Management

**Location:** `RefreshTokenService.java` | `RefreshTokenServiceImpl.java`

**Purpose:** Extend user session without re-authentication

**Flow:**
1. Login generates JWT + Refresh Token
2. When JWT expires, client sends Refresh Token
3. System validates token and generates new JWT
4. Old Refresh Token revoked (logout) or persisted

**Storage:** Database table `refresh_tokens` with token, user, expiry

**Security Features:**
- Tokens stored with expiry timestamp
- Revocation mechanism on logout
- Single-use tokens (recommended)

---

## API Endpoints - Detailed Analysis

### CONTROLLER 1: AuthController (`/auth`)

#### 1.1 User Registration
**Endpoint:** `POST /auth/register`  
**Authentication:** ❌ Not Required  
**Rate Limit:** Default

**Request:**
```json
{
  "username": "john_doe",
  "password": "secure_password",
  "roles": ["VIEWER"],
  "team": "TeamA"
}
```

**Response:**
- **201 Created:** Empty response body
- **400 Bad Request:** Validation errors

**Business Logic:**
1. Validate request body (username, password, roles, team)
2. Hash password using BCrypt
3. Create UserEntity with provided details
4. Save to users table (unique constraint on username)
5. Add user roles to user_roles table

**Purpose:** User account creation for system access

**Approach to Implement:**
- Use PasswordEncoder for BCrypt hashing (spring-security-crypto)
- UserDao handles database persistence
- Roles stored as separate records in user_roles junction table

**Edge Cases:**
- Duplicate username → DataIntegrityViolationException caught (HTTP 400)
- Null/empty username → Validation error
- Invalid role format → Persisted as-is (no validation)
- Team not provided → Allows null team (potential bug)

**Exception Handling:**
```
DataIntegrityViolationException (duplicate username)
  ↓
GlobalExceptionHandler.handleConstraint()
  ↓
HTTP 400: "Invalid request data"
```

---

#### 1.2 User Login
**Endpoint:** `POST /auth/login`  
**Authentication:** ❌ Not Required  
**Rate Limit:** Default

**Request:**
```json
{
  "username": "john_doe",
  "password": "secure_password"
}
```

**Response:**
```json
{
  "jwtToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Business Logic:**
1. Validate credentials (username & password not null)
2. Authenticate using AuthenticationManager
   - AuthenticationManager delegates to CustomUserDetailsService
   - CustomUserDetailsService loads UserEntity by username
   - PasswordEncoder verifies password against hash
3. On success:
   - Fetch UserEntity from database
   - Generate JWT token (15 min expiry)
   - Create and return RefreshToken
4. On failure: BadCredentialsException raised

**Purpose:** User authentication and session token generation

**Approach to Implement:**
- Spring Security's AuthenticationManager handles credential verification
- JwtUtil generates signed tokens with user roles
- RefreshTokenService creates database record for token revocation tracking

**Data Flow:**
```
POST /auth/login
  ├─ Request validation (credentials not null)
  ├─ AuthenticationManager.authenticate()
  │   ├─ CustomUserDetailsService.loadUserByUsername()
  │   │   └─ UserDao.findByUsername() → UserDetails
  │   └─ PasswordEncoder.matches(plaintext, hash)
  ├─ JwtUtil.generateToken(user)
  │   └─ Jwts.builder() → signed token
  ├─ RefreshTokenService.create(user)
  │   └─ Save RefreshToken entity with expiry
  └─ Return AuthResponse(jwt, refresh)
```

**Edge Cases:**
- Null username/password → BadCredentialsException (HTTP 401)
- Non-existent user → AuthenticationManager returns failed auth
- Incorrect password → BadCredentialsException (HTTP 401)
- User roles empty → JWT generated with empty roles claim
- Database unavailable → Database exception propagated

**Exception Handling:**
```
BadCredentialsException
  ↓
GlobalExceptionHandler.badCreds()
  ↓
HTTP 401: "Invalid credentials"
```

---

#### 1.3 Token Refresh
**Endpoint:** `POST /auth/refresh`  
**Authentication:** ❌ Not Required  
**Rate Limit:** Default

**Request:**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:**
```json
{
  "jwtToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Business Logic:**
1. Receive refresh token UUID
2. RefreshTokenService.verify(token):
   - Query refresh_tokens table by token ID
   - Check if token is revoked (status = REVOKED)
   - Check if token expiry > current time
   - Return RefreshToken entity with associated user
3. Generate new JWT for user
4. Return new JWT + same refresh token

**Purpose:** Extend user session without requiring password re-entry

**Approach to Implement:**
- Refresh tokens stored in database with expiry & status
- Can be single-use or reusable (current: reusable)
- Calling this endpoint multiple times returns same refresh token

**Edge Cases:**
- Token not found → EntityNotFoundException (HTTP 404)
- Token expired → Exception thrown
- Token revoked → Exception on logout
- Invalid UUID format → Invalid argument exception

**Exception Handling:**
```
EntityNotFoundException
  ↓
GlobalExceptionHandler.handleEntityNotFound()
  ↓
HTTP 404: "Refresh token not found"
```

---

#### 1.4 User Logout
**Endpoint:** `POST /auth/logout`  
**Authentication:** ❌ Not Required  
**Rate Limit:** Default

**Request:**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:**
- **204 No Content:** Empty response

**Business Logic:**
1. Receive refresh token
2. RefreshTokenService.revoke(token):
   - Find RefreshToken by ID
   - Set status to REVOKED
   - Update last_modified timestamp
   - Save to database
3. Future attempts to use this token will fail

**Purpose:** Invalidate refresh token, forcing re-login requirement

**Approach to Implement:**
- Soft-delete pattern using status column
- No hard delete to maintain audit trail

**Edge Cases:**
- Token already revoked → Allowed (idempotent)
- Token not found → Silently succeeds
- Invalid UUID → Invalid argument exception

**Exception Handling:**
```
Exceptions suppressed (idempotent operation)
  ↓
HTTP 204: No Content (always)
```

---

### CONTROLLER 2: ServiceCatController (`/api/services`)

Service Catalog management for registering applications/services in the system.

#### 2.1 Create Service
**Endpoint:** `POST /api/services`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** Any authenticated user  
**Rate Limit:** Default (60 burst, 20 tokens/sec)

**Request:**
```json
{
  "serviceName": "user-service",
  "repoUrl": "https://github.com/company/user-service.git",
  "ownerTeam": "TeamA",
  "runTime": "Java 17",
  "status": "ACTIVE"
}
```

**Response:**
```json
{
  "serviceId": "550e8400-e29b-41d4-a716-446655440000",
  "serviceName": "user-service",
  "repoUrl": "https://github.com/company/user-service.git",
  "ownerTeam": "TeamA",
  "runTime": "Java 17",
  "status": "ACTIVE",
  "createdAt": "2026-04-25T10:30:00Z"
}
```

**Status:** ✅ HTTP 201 Created | ❌ HTTP 400 Bad Request

**Business Logic:**
1. Validate request (serviceName required, unique check)
2. Check if service already exists by name
3. Create ServiceCatInfo entity
4. Set additional fields:
   - serviceId: Auto-generated UUID
   - createdAt: Current timestamp
5. Persist to `services` table
6. Map to response DTO
7. Audit: Automatically logged via @Auditable (if applied)

**Purpose:** Register a new service/application in the catalog

**Approach to Implement:**
- Uniqueness enforced via database constraint
- Service name is business key
- UUIDs for system-wide unique identification
- Mapper pattern for DTO conversion

**Data Flow:**
```
POST /api/services
  ├─ Validation
  ├─ ServiceCatDao.existsByName(serviceName)
  ├─ ServiceCatMapper.toEntity(request)
  ├─ ServiceCatDao.save(entity)
  ├─ ServiceCatMapper.toResponse(saved)
  └─ HTTP 201 Created
```

**Edge Cases:**
- Duplicate service name → DataIntegrityViolationException (HTTP 400)
- Null serviceName → MethodArgumentNotValidException (HTTP 400)
- Invalid team format → Persisted as-is (no validation)
- Null runtime → Allowed (nullable column)
- Missing fields → Validation errors with field-level messages

**Exception Handling:**
```
DataIntegrityViolationException (duplicate name)
  ↓
GlobalExceptionHandler.handleConstraint()
  ↓
HTTP 400: "Invalid request data"

MethodArgumentNotValidException (validation fail)
  ↓
GlobalExceptionHandler.handleValidation()
  ↓
HTTP 400: { "serviceName": "must not be blank" }
```

---

#### 2.2 Get All Services
**Endpoint:** `GET /api/services`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** Any authenticated user  
**Rate Limit:** Default

**Response:**
```json
[
  {
    "serviceId": "550e8400-e29b-41d4-a716-446655440000",
    "serviceName": "user-service",
    "repoUrl": "https://github.com/company/user-service.git",
    "ownerTeam": "TeamA",
    "runTime": "Java 17",
    "status": "ACTIVE",
    "createdAt": "2026-04-25T10:30:00Z"
  },
  {
    "serviceId": "550e8400-e29b-41d4-a716-446655440001",
    "serviceName": "payment-service",
    "repoUrl": "https://github.com/company/payment-service.git",
    "ownerTeam": "TeamB",
    "runTime": "Java 21",
    "status": "ACTIVE",
    "createdAt": "2026-04-24T15:20:00Z"
  }
]
```

**Business Logic:**
1. Query all services from database
2. Map each entity to response DTO
3. Return as JSON array

**Purpose:** Retrieve complete list of all services in catalog

**Approach to Implement:**
- Simple repository query (no filtering)
- All services returned regardless of team ownership
- No pagination (potential performance issue with large datasets)

**Edge Cases:**
- Empty service catalog → Empty array returned
- Null fields in entity → Null values in response
- Large dataset (1000+ services) → Slow response, memory intensive

**Exception Handling:**
```
Database exceptions
  ↓
Spring exception handling
  ↓
HTTP 500 Internal Server Error
```

---

#### 2.3 Get Service by ID
**Endpoint:** `GET /api/services/{id}`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** Any authenticated user  
**Rate Limit:** Default

**Path Parameter:** `id` = Service UUID

**Response (Success):**
```json
{
  "serviceId": "550e8400-e29b-41d4-a716-446655440000",
  "serviceName": "user-service",
  ...
}
```

**Status:** ✅ HTTP 200 OK | ❌ HTTP 404 Not Found

**Business Logic:**
1. Extract serviceId from path parameter
2. Query database by ID
3. If found: map to response DTO
4. If not found: throw EntityNotFoundException

**Purpose:** Retrieve details of specific service

**Approach to Implement:**
- Primary key lookup (efficient)
- Single service entity retrieved

**Edge Cases:**
- Invalid UUID format → Invalid argument exception
- Service deleted from database → EntityNotFoundException
- Null response from DAO → NullPointerException

**Exception Handling:**
```
EntityNotFoundException
  ↓
GlobalExceptionHandler.handleEntityNotFound()
  ↓
HTTP 404: "Service not found"
```

---

#### 2.4 List Services with Filtering & Pagination
**Endpoint:** `GET /api/services/all`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** Any authenticated user  
**Rate Limit:** Default

**Query Parameters:**
- `runtime` (optional): Filter by runtime (e.g., "Java 17")
- `status` (optional): Filter by status (e.g., "ACTIVE")
- `ownerTeam` (optional): Filter by team
- `page` (optional): Page number (0-indexed, default 0)
- `size` (optional): Page size (default 20)
- `sort` (optional): Sort criteria (e.g., "createdAt,desc")

**Response:**
```json
{
  "content": [
    {
      "serviceId": "550e8400-e29b-41d4-a716-446655440000",
      "serviceName": "user-service",
      ...
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "offset": 0,
    "sorted": true,
    "unpaged": false
  },
  "totalPages": 5,
  "totalElements": 95,
  "last": false,
  "size": 20,
  "number": 0,
  "sort": { "empty": false, "sorted": true, "unsorted": false },
  "numberOfElements": 20,
  "first": true,
  "empty": false
}
```

**Business Logic:**
1. Extract filter parameters from query
2. Build dynamic JPA Specification:
   - Add runtime filter if provided (null-safe)
   - Add status filter if provided (null-safe)
   - Enforce team filter for non-admin users (TBAC)
   - Allow team override for admin users
3. Execute paginated query with specifications
4. Map results to response DTOs
5. Return Page object (Spring auto-serializes)

**Security Check:**
```java
boolean isAdmin = SecurityUtil.isAdmin();

if (!isAdmin) {
    // Non-admin: enforce their team filter
    spec = spec.and(teamEquals(SecurityUtil.currentUserTeam()));
} else if (ownerTeam != null) {
    // Admin: apply requested team filter if provided
    spec = spec.and(teamEquals(ownerTeam));
}
```

**Purpose:** 
- Retrieve paginated list of services
- Apply role-based filtering (Team-Based Access Control)
- Support advanced search/filtering

**Approach to Implement:**
- Spring Data JPA Specification pattern for dynamic queries
- SpecUtil builder for null-safe clause composition
- Pageable abstraction for pagination
- TBAC enforced at query layer

**Data Flow:**
```
GET /api/services/all?runtime=Java17&page=0&size=20
  ├─ Extract parameters
  ├─ Build Specification
  │   ├─ runtime filter
  │   ├─ status filter
  │   └─ TBAC team filter
  ├─ ServiceCatDao.findAll(spec, pageable)
  ├─ Map to response DTOs
  └─ Return Page<ServiceCatResponse>
```

**Edge Cases:**
- Invalid page number → Spring returns empty page
- Page > totalPages → Empty content with correct metadata
- Invalid sort parameter → IllegalArgumentException
- Both admin & non-admin accessing same endpoint:
  - Admin sees all services (can filter by team)
  - Non-admin sees only team's services (team filter enforced)
- Null runtime/status → Filter ignored (SpecUtil handles null-safety)
- Case-sensitive filters → Database default collation applies

**Exception Handling:**
```
IllegalArgumentException (invalid sort)
  ↓
GlobalExceptionHandler.badRequest()
  ↓
HTTP 400: Error message
```

---

#### 2.5 Update Service
**Endpoint:** `PUT /api/services/{id}`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** ADMIN or team owner (via OwnershipGuard)  
**Rate Limit:** Default  
**Audit:** ✅ Logged via @Auditable

**Request:**
```json
{
  "serviceName": "user-service",
  "repoUrl": "https://github.com/company/user-service-v2.git",
  "ownerTeam": "TeamA",
  "runTime": "Java 21",
  "status": "MAINTENANCE"
}
```

**Response:**
```json
{
  "serviceId": "550e8400-e29b-41d4-a716-446655440000",
  "serviceName": "user-service",
  ...
}
```

**Status:** ✅ HTTP 200 OK | ❌ HTTP 403 Forbidden | ❌ HTTP 404 Not Found | ❌ HTTP 400 Bad Request

**Business Logic:**
1. Extract service ID from path
2. Fetch service from database
3. **Authorization Check (OwnershipGuard):**
   - If ADMIN: Allow update
   - If non-admin:
     - Compare user's team with service's ownerTeam (case-insensitive)
     - If teams don't match: Throw AccessDeniedException (HTTP 403)
4. Update service fields:
   - repoUrl
   - ownerTeam
   - runTime
   - status
   (serviceName & createdAt typically not updated)
5. Save updated entity
6. **Audit Logging (AuditAspect):**
   - Intercept method via @Auditable annotation
   - Record: actor, action="UPDATE_SERVICE", resource, resourceId, outcome
   - Log success or failure (if exception thrown)
7. Return updated service response

**Purpose:** Modify service configuration and metadata

**Approach to Implement:**
- OwnershipGuard enforces TBAC at service layer
- ServiceCatMapper.updateEntity() applies field changes
- @Auditable annotation triggers audit logging via AuditAspect

**Security Flow:**
```
PUT /api/services/{id}
  ├─ JWT validation (JwtFilter)
  ├─ Fetch service by ID
  ├─ OwnershipGuard.assertCanModify(service)
  │   ├─ If ADMIN: return (allow)
  │   └─ If not ADMIN:
  │       ├─ Get user's team
  │       ├─ Compare with service.ownerTeam
  │       └─ If mismatch: throw AccessDeniedException
  ├─ ServiceCatMapper.updateEntity(service, request)
  ├─ ServiceCatDao.save(service)
  ├─ AuditAspect @AfterReturning intercept
  │   └─ Log success: actor, action, resourceId
  └─ Return response
```

**Edge Cases:**
- Service not found → EntityNotFoundException (HTTP 404)
- User not ADMIN and team doesn't match → AccessDeniedException (HTTP 403)
- Non-existent user in SecurityContext → NullPointerException
- Update fails (e.g., duplicate serviceName if allowing name changes) → Constraint violation
- AuditLog creation fails → Audit exception (depends on aspect handling)

**Exception Handling:**
```
AccessDeniedException
  ↓
Spring Security exception handling
  ↓
HTTP 403: Forbidden

EntityNotFoundException
  ↓
GlobalExceptionHandler.handleEntityNotFound()
  ↓
HTTP 404: "Service not found"

During @Auditable:
Exception (any)
  ↓
AuditAspect @AfterThrowing intercept
  ↓
Log failure with error message
  ↓
Exception re-thrown
```

---

#### 2.6 Delete Service
**Endpoint:** `DELETE /api/services/{id}`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** ADMIN or team owner (via OwnershipGuard)  
**Rate Limit:** Default  
**Audit:** ✅ Logged via @Auditable

**Response:**
- **204 No Content:** Empty response

**Status:** ✅ HTTP 204 No Content | ❌ HTTP 403 Forbidden | ❌ HTTP 404 Not Found

**Business Logic:**
1. Extract service ID from path
2. Fetch service from database
3. **Authorization Check (OwnershipGuard):**
   - Same as update (ADMIN bypass, team ownership check)
4. Delete service from database (hard delete)
5. **Audit Logging:**
   - Record: action="DELETE_SERVICE", resourceId, outcome

**Purpose:** Remove service from catalog

**Approach to Implement:**
- OwnershipGuard enforces authorization
- Hard delete (removes record, not soft-delete)

**Edge Cases:**
- Service not found → EntityNotFoundException (HTTP 404)
- Authorization check fails → AccessDeniedException (HTTP 403)
- Cascading deletes fail (FK constraints) → DataIntegrityViolationException
- Audit logging fails → Depends on aspect error handling

**Exception Handling:**
```
Same as Update endpoint
```

---

### CONTROLLER 3: DeploymentController (`/api/deployments`)

Manages deployment records and version history tracking.

#### 3.1 Register Deployment
**Endpoint:** `POST /api/deployments`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** ✅ ADMIN only  
**Rate Limit:** Default  
**Audit:** ✅ Logged via @Auditable

**Request:**
```json
{
  "serviceId": "550e8400-e29b-41d4-a716-446655440000",
  "version": "2.5.0",
  "env": "production",
  "status": "SUCCESS",
  "deployedAt": "2026-04-25T10:30:00Z",
  "triggeredBy": "jenkins-ci"
}
```

**Response:**
- **202 Accepted:** Empty response

**Business Logic:**
1. Validate request body (all required fields)
2. Map request to DeploymentEntity
3. Save to deployments table
4. **Audit Logging:**
   - action="DEPLOY", resource="SERVICE"
   - resourceId extracted from serviceId parameter

**Purpose:** Record a service deployment event

**Approach to Implement:**
- DeploymentMapper handles entity mapping
- Timestamp stored with deployment record
- No business validation (service existence not checked)

**Edge Cases:**
- Invalid UUID format → Invalid argument exception
- Null version → Validation error
- Duplicate deployment → Allowed (multiple deployments per version possible)
- Service doesn't exist → No check performed (referential integrity depends on DB constraint)

**Exception Handling:**
```
MethodArgumentNotValidException (validation fail)
  ↓
GlobalExceptionHandler.handleValidation()
  ↓
HTTP 400: Field errors
```

---

#### 3.2 Get Deployment History
**Endpoint:** `GET /api/deployments/{serviceId}`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** ADMIN or VIEWER  
**Rate Limit:** Default

**Path Parameter:** `serviceId` = Service UUID

**Query Parameters:**
- `page` (optional): Page number (default 0)
- `size` (optional): Page size (default 20)

**Response:**
```json
{
  "content": [
    {
      "deploymentId": "550e8400-e29b-41d4-a716-446655440000",
      "serviceId": "550e8400-e29b-41d4-a716-446655440001",
      "version": "2.5.0",
      "env": "production",
      "status": "SUCCESS",
      "deployedAt": "2026-04-25T10:30:00Z",
      "triggeredBy": "jenkins-ci"
    },
    {
      "deploymentId": "550e8400-e29b-41d4-a716-446655440002",
      "serviceId": "550e8400-e29b-41d4-a716-446655440001",
      "version": "2.4.5",
      "env": "production",
      "status": "SUCCESS",
      "deployedAt": "2026-04-24T08:15:00Z",
      "triggeredBy": "jenkins-ci"
    }
  ],
  "totalPages": 10,
  "totalElements": 192,
  ...
}
```

**Business Logic:**
1. Extract service ID from path
2. Query deployments filtered by serviceId
3. Apply pagination
4. Map results to response DTOs
5. Return paginated Page object

**Purpose:** View version deployment history for a service

**Approach to Implement:**
- DeploymentDao.findByService(serviceId, pageable) returns sorted (newest first)
- Simple repository query with pagination

**Edge Cases:**
- Service has no deployments → Empty page returned
- Invalid serviceId UUID → Invalid argument exception
- Negative page number → Spring converts to default (0)

---

#### 3.3 Get Latest Deployment
**Endpoint:** `GET /api/deployments/{serviceId}/latest`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** ADMIN or VIEWER  
**Rate Limit:** Default

**Query Parameters:**
- `env` (optional): Filter by environment (e.g., "production", "staging")

**Response:**
```json
{
  "deploymentId": "550e8400-e29b-41d4-a716-446655440000",
  "serviceId": "550e8400-e29b-41d4-a716-446655440001",
  "version": "2.5.0",
  "env": "production",
  "status": "SUCCESS",
  "deployedAt": "2026-04-25T10:30:00Z",
  "triggeredBy": "jenkins-ci"
}
```

**Status:** ✅ HTTP 200 OK | ❌ HTTP 404 Not Found

**Business Logic:**
1. If `env` parameter provided:
   - Query latest deployment for service in specific environment
   - DeploymentDao.latestByEnv(serviceId, env)
2. If `env` not provided:
   - Query latest deployment for service across all environments
   - DeploymentDao.latest(serviceId)
3. Map result to response or null if not found

**Purpose:** Quick lookup of current/latest deployment state

**Approach to Implement:**
- Database query with ORDER BY deployedAt DESC LIMIT 1
- Optional parameter handling with ternary operator

**Edge Cases:**
- No deployments exist → Null returned → Will return HTTP 500 (NullPointerException)
- Invalid environment → Empty result set → Null returned

---

#### 3.4 Get Deployment Summary
**Endpoint:** `GET /api/deployments/{serviceId}/summary`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** ADMIN or VIEWER  
**Rate Limit:** Default

**Response:**
```json
{
  "deploymentsByEnv": {
    "production": {
      "version": "2.5.0",
      "status": "SUCCESS",
      "deployedAt": "2026-04-25T10:30:00Z",
      "triggeredBy": "jenkins-ci"
    },
    "staging": {
      "version": "2.6.0-beta",
      "status": "IN_PROGRESS",
      "deployedAt": "2026-04-25T09:00:00Z",
      "triggeredBy": "jenkins-ci"
    },
    "development": {
      "version": "main-build-123",
      "status": "SUCCESS",
      "deployedAt": "2026-04-25T05:30:00Z",
      "triggeredBy": "developer"
    }
  }
}
```

**Business Logic:**
1. Query all deployments for service
2. Group deployments by environment
3. For each environment, keep only the first occurrence (putIfAbsent)
   - Since query likely returns newest-first, first occurrence = latest per env
4. Return map of environments to deployment summaries

**Purpose:** Show current deployed version across all environments

**Approach to Implement:**
- DeploymentDao.findAllByService(serviceId) returns all deployments
- Group using HashMap with putIfAbsent (ensures latest per env)
- DeploySummaryResponse.DeploymentEnvSummary DTO for environment summary

**Edge Cases:**
- Service has no deployments → Empty map returned
- Service appears in multiple environments → Each tracked separately
- Duplicate environment entries → First (newest) entry wins

---

#### 3.5 Rollback Deployment
**Endpoint:** `POST /api/deployments/rollback`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** ✅ ADMIN only  
**Rate Limit:** Default

**Request:**
```json
{
  "serviceId": "550e8400-e29b-41d4-a716-446655440000",
  "env": "production",
  "version": "2.4.5",
  "triggeredBy": "admin@company.com"
}
```

**Response:**
- **202 Accepted:** Empty response

**Business Logic:**
1. Validate request body
2. Create new DeploymentEntity with:
   - serviceId, env, version from request
   - status: "ROLLED_BACK"
   - triggeredBy: from request
3. Save as new deployment record (not updating existing)

**Purpose:** Record a deployment rollback action

**Approach to Implement:**
- Rollback creates audit trail entry (new deployment record)
- Doesn't actually revert application code (only records intention)
- Actual rollback executed by deployment pipeline

**Edge Cases:**
- Rolling back to non-existent version → Allowed (no validation)
- Same version already deployed → Allowed
- Multiple rollbacks on same env → Creates multiple records

---

### CONTROLLER 4: MetricController (`/api/metrics`)

Handles metrics ingestion and health monitoring.

#### 4.1 Ingest Metrics
**Endpoint:** `POST /api/metrics/ingest/{serviceId}`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** ✅ ADMIN only  
**Rate Limit:** ⏱️ Stricter (20 burst, 5 tokens/sec)  
**Async:** ✅ Submitted to queue

**Request:**
```json
{
  "cpuUsage": 45.2,
  "memoryUsageMb": 512,
  "timestamp": "2026-04-25T10:30:00Z"
}
```

**Response:**
- **202 Accepted:** Empty response (async submission)

**Business Logic:**
1. Receive metric request
2. **Async Submission via AsyncIngestor:**
   - If Kafka enabled: Submit to `metric-topic` queue
   - If Kafka disabled: Execute synchronous ingest
3. Endpoint returns immediately (202 Accepted)
4. **Async Processing (MetricConsumer or Synchronous):**
   - Fetch service by serviceId
   - Map request to MetricEntity
   - Save to metrics table with current timestamp

**Purpose:** Record performance metrics for services

**Approach to Implement:**
- AsyncIngestor pattern with strategy (Kafka or sync)
- Kafka producer for high-throughput scenarios
- Metrics associated with service via serviceId

**Rate Limiting Rationale:** 
- High-volume ingestion endpoint
- Stricter limits to prevent system overload
- 5 tokens/sec = 300 metrics per minute max

**Edge Cases:**
- Service doesn't exist → Handled by async consumer or silent fail
- Invalid metrics (negative CPU, etc.) → Stored as-is (no validation)
- Kafka queue full → Message dropped or exception in async handler
- Timestamp in future → Allowed (no validation)

**Exception Handling:**
```
Validation errors
  ↓
GlobalExceptionHandler.handleValidation()
  ↓
HTTP 400: Field errors

(Async errors handled in consumer, not returned to client)
```

---

#### 4.2 Send Heartbeat
**Endpoint:** `POST /api/metrics/heartbeat/{serviceId}`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** ✅ ADMIN only  
**Rate Limit:** Default

**Response:**
- **202 Accepted:** Empty response

**Business Logic:**
1. Create minimal MetricEntity with:
   - serviceId from path
   - cpuUsage: null or 0
   - memoryUsageMb: null or 0
   - timestamp: current time
2. Save to metrics table
3. Purpose: Update "last seen" timestamp for service

**Purpose:** Ping a service to indicate it's alive (no actual metrics)

**Approach to Implement:**
- MetricMapper.heartbeat() creates minimal entity
- Used for health check tracking

**Edge Cases:**
- Service doesn't exist → No validation, saved anyway
- Heartbeat without metrics → Registered as null/zero usage

---

#### 4.3 Get Latest Metric
**Endpoint:** `GET /api/metrics/{serviceId}/latest`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** ADMIN or VIEWER  
**Rate Limit:** Default

**Response:**
```json
{
  "metricId": "550e8400-e29b-41d4-a716-446655440000",
  "serviceId": "550e8400-e29b-41d4-a716-446655440001",
  "cpuUsage": 45.2,
  "memoryUsageMb": 512,
  "timestamp": "2026-04-25T10:30:00Z"
}
```

**Business Logic:**
1. Query latest metric for service
2. DetricsDao.findLatest(serviceId)
3. Map to response DTO

**Purpose:** Quick lookup of most recent service metrics

---

#### 4.4 Get Metric History
**Endpoint:** `GET /api/metrics/{serviceId}`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** Any authenticated user  
**Rate Limit:** Default

**Query Parameters:**
- `page` (optional): Page number (default 0)
- `size` (optional): Page size (default 20)

**Response:**
```json
{
  "content": [
    { "metricId": "...", "cpuUsage": 45.2, ... },
    { "metricId": "...", "cpuUsage": 42.1, ... }
  ],
  "totalPages": 50,
  ...
}
```

**Business Logic:**
1. Query metrics for service with pagination
2. Return sorted by timestamp (newest first)

**Purpose:** Retrieve metrics history for trend analysis

---

#### 4.5 Get Service Health
**Endpoint:** `GET /api/metrics/{serviceId}/health`  
**Authentication:** ❌ Not Required (Public)  
**Authorization:** Public API  
**Rate Limit:** Default

**Response:**
```json
{
  "serviceId": "550e8400-e29b-41d4-a716-446655440000",
  "lastSeen": "2026-04-25T10:30:00Z",
  "isHealthy": true,
  "secondsSinceLastUpdate": 45
}
```

**Business Logic:**
1. Query latest metric for service
2. Calculate seconds elapsed since metric timestamp:
   ```
   diff = now - lastMetricTimestamp
   ```
3. Determine health status:
   - **HEALTHY:** diff ≤ 60 seconds
   - **DEGRADED:** 60 < diff ≤ 180 seconds
   - **DOWN:** diff > 180 seconds
4. If no metrics exist: Return isHealthy=false, secondsSinceLastUpdate=-1

**Purpose:** Public health check endpoint (e.g., for external monitoring)

**Edge Cases:**
- No metrics for service → isHealthy=false
- Metric timestamp in future → Negative diff (edge case)
- Clock skew between servers → Potential false DOWN status

**Business Logic Detail - Health Thresholds:**
```
if diff <= 60s: HEALTHY (service actively reporting)
elif 60 < diff <= 180s: DEGRADED (delay in metric reporting)
elif diff > 180s: DOWN (no metrics for 3+ minutes)
```

---

#### 4.6 Get Metrics Summary
**Endpoint:** `GET /api/metrics/{serviceId}/summary`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** ADMIN or VIEWER  
**Rate Limit:** Default

**Query Parameters:**
- `window` (optional): Time window in minutes (default 15)
- `from` (optional): Start timestamp (Instant format)
- `to` (optional): End timestamp (Instant format)

**Response:**
```json
{
  "serviceId": "550e8400-e29b-41d4-a716-446655440000",
  "window": 15,
  "status": "HEALTHY",
  "avgCpu": 42.5,
  "maxCpu": 78.3,
  "minCpu": 15.2,
  "avgMemory": 512.0,
  "maxMemory": 1024.0,
  "minMemory": 256.0,
  "lastSeen": "2026-04-25T10:30:00Z",
  "dataPoints": 30
}
```

**Business Logic:**
1. Determine time window:
   - If `from` provided: Use as start
   - If `from` not provided:
     - Use `window` parameter (default 15 minutes)
     - Calculate: effectiveFrom = now - window minutes
2. Determine end time:
   - If `to` provided: Use as end
   - Otherwise: Use now
3. Query metrics for service within [effectiveFrom, effectiveTo]
4. If no data: Return SummaryResponse.noData()
5. Aggregate metrics:
   - Average CPU usage
   - Min/Max CPU usage
   - Average memory usage
   - Min/Max memory usage
   - Last seen timestamp
   - Data point count
6. Calculate health status based on lastSeen:
   - ≤ 60s: HEALTHY
   - 61-180s: DEGRADED
   - > 180s: DOWN
7. Return SummaryResponse with status

**Purpose:** Time-based metrics aggregation for trend analysis

**Data Query (Custom Projection):**
```sql
SELECT 
  AVG(cpuUsage) as avgCpu,
  MAX(cpuUsage) as maxCpu,
  MIN(cpuUsage) as minCpu,
  AVG(memoryUsageMb) as avgMemory,
  MAX(memoryUsageMb) as maxMemory,
  MIN(memoryUsageMb) as minMemory,
  MAX(timestamp) as lastSeen,
  COUNT(*) as dataPoints
FROM metrics
WHERE serviceId = ? AND timestamp BETWEEN ? AND ?
```

**Edge Cases:**
- `window` and `from` both provided: `from` takes precedence
- Invalid window (negative, 0): Likely causes error (no validation)
- No metrics in window: Returns summary with nulls/NO_DATA status
- End time before start time: Empty result set
- Future window specified: No data returned

---

### CONTROLLER 5: LogsController (`/api/logs`)

Application log ingestion and retrieval.

#### 5.1 Ingest Logs
**Endpoint:** `POST /api/logs/ingest/{serviceId}`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** ✅ ADMIN only  
**Rate Limit:** ⏱️ Stricter (30 burst, 10 tokens/sec)  
**Async:** ✅ Submitted to queue

**Request:**
```json
{
  "message": "Application started successfully",
  "level": "INFO",
  "timestamp": "2026-04-25T10:30:00Z",
  "source": "ApplicationStartup"
}
```

**Response:**
- **202 Accepted:** Empty response (async submission)

**Business Logic:**
1. Receive log request
2. Submit to AsyncIngestor (Kafka or sync)
3. Return 202 immediately
4. Async processor:
   - Fetch service by serviceId
   - Map to LogEntity
   - Save to logs table

**Purpose:** Collect application logs for auditing and debugging

**Rate Limiting:** 10 tokens/sec = 600 logs per minute max

---

#### 5.2 Get Logs with Filtering
**Endpoint:** `GET /api/logs/{serviceId}`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** ADMIN or VIEWER  
**Rate Limit:** Default

**Query Parameters:**
- `from` (optional): Start timestamp
- `to` (optional): End timestamp
- `level` (optional): Log level filter (INFO, WARN, ERROR, etc.)
- `page` (optional): Page number
- `size` (optional): Page size

**Response:**
```json
{
  "content": [
    {
      "logId": "550e8400-e29b-41d4-a716-446655440000",
      "serviceId": "550e8400-e29b-41d4-a716-446655440001",
      "message": "Application started successfully",
      "level": "INFO",
      "timestamp": "2026-04-25T10:30:00Z",
      "source": "ApplicationStartup"
    }
  ],
  "totalPages": 10,
  ...
}
```

**Business Logic:**
1. Build dynamic JPA Specification:
   - Always filter by serviceId (required)
   - Add level filter if provided
   - Add timestamp >= from if provided
   - Add timestamp <= to if provided
2. Execute paginated query
3. Return mapped Page<LogResponse>

**Purpose:** Retrieve logs with time range and level filtering

---

### CONTROLLER 6: AlertController (`/api/alerts`)

Alert evaluation and lifecycle management.

#### 6.1 Evaluate Alerts
**Endpoint:** `POST /api/alerts/evaluate/{serviceId}`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** ✅ ADMIN only  
**Rate Limit:** Default

**Response:**
- **200 OK:** Empty response

**Business Logic:**
1. Get metrics summary for service (15-minute window)
2. Check for "NO_DATA" status → Return early (can't evaluate without data)
3. Evaluate alert rules via AlertRuleEngine:
   - **CPU_HIGH:** cpuUsage > 80% → Severity: MEDIUM
   - **MEMORY_HIGH:** memoryUsage > 1024 MB → Severity: MEDIUM
   - **SERVICE_DOWN:** status = "DOWN" → Severity: HIGH
4. For each triggered condition:
   - Check if open alert already exists for service + alert type
   - If exists: Skip (prevent duplicate alerts)
   - If not exists: Create new AlertEntity with status="OPEN"
5. Save alert to database

**Purpose:** Automated alert generation based on metric thresholds

**Alert Rules:**
```
rule CPU_HIGH: IF avg_cpu > 80% THEN trigger(severity=MEDIUM)
rule MEMORY_HIGH: IF avg_memory > 1024MB THEN trigger(severity=MEDIUM)
rule SERVICE_DOWN: IF status = DOWN THEN trigger(severity=HIGH)
```

**Deduplication Logic:**
```java
if (!dao.hasOpenAlert(serviceId, type)) {
    // Create new alert only if no open alert exists
    dao.save(newAlert);
}
```

**Edge Cases:**
- No metrics available → Early return (no alerts)
- Multiple threshold violations → Multiple alerts created
- Alert already open for condition → No duplicate created
- Service down but metrics exist → DOWN alert triggered

---

#### 6.2 Get All Open Alerts
**Endpoint:** `GET /api/alerts`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** ADMIN or VIEWER  
**Rate Limit:** Default

**Response:**
```json
[
  {
    "alertId": "550e8400-e29b-41d4-a716-446655440000",
    "serviceId": "550e8400-e29b-41d4-a716-446655440001",
    "type": "CPU_HIGH",
    "severity": "MEDIUM",
    "message": "CPU usage exceeded threshold",
    "status": "OPEN",
    "createdAt": "2026-04-25T10:30:00Z",
    "resolvedAt": null
  }
]
```

**Business Logic:**
1. Query all alerts with status="OPEN"
2. Map to response DTOs
3. Return as array

**Purpose:** Dashboard view of current active alerts

---

#### 6.3 Get Service-Specific Alerts
**Endpoint:** `GET /api/alerts/{serviceId}`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** ADMIN or VIEWER  
**Rate Limit:** Default

**Response:** Same as 6.2 but filtered by serviceId

**Business Logic:**
1. Query alerts for specific service (all statuses)
2. Map to response DTOs

**Purpose:** View alert history for specific service

---

#### 6.4 Resolve Alert
**Endpoint:** `POST /api/alerts/{id}/resolve`  
**Authentication:** ✅ Required (JWT)  
**Authorization:** ADMIN or VIEWER  
**Rate Limit:** Default

**Response:**
- **200 OK:** Empty response

**Business Logic:**
1. Fetch alert by ID
2. Update alert:
   - status: "RESOLVED"
   - resolvedAt: Current timestamp
3. Save to database

**Purpose:** Mark alert as resolved/acknowledged

**Edge Cases:**
- Alert already resolved → Updates resolvedAt again (idempotent)
- Alert not found → NullPointerException (should handle)
- Multiple resolves → Last resolve timestamp wins

---

## Exception Handling Strategy

### Global Exception Handler
**Location:** `GlobalExceptionHandler.java`

**Responsibility:** Centralized exception-to-HTTP response mapping

### Exception Mappings

| Exception | HTTP Status | Response Body | Use Case |
|-----------|------------|---------------|----------|
| `DataIntegrityViolationException` | 400 | "Invalid request data" | Constraint violations (duplicate, FK error) |
| `MethodArgumentNotValidException` | 400 | Field-level errors | Request validation failures |
| `EntityNotFoundException` | 404 | Error message | Resource not found |
| `BadCredentialsException` | 401 | "Invalid credentials" | Wrong username/password |
| `IllegalArgumentException` | 400 | Message | Invalid arguments |
| `TokenException` | 401 | Error message | JWT validation failure |

### Exception Handling Flows

#### Authentication Failures
```
Login with wrong password
  ↓
AuthenticationManager throws BadCredentialsException
  ↓
GlobalExceptionHandler.badCreds()
  ↓
HTTP 401: "Invalid credentials"
```

#### Validation Failures
```
POST /api/services with null serviceName
  ↓
@Valid annotation triggers validation
  ↓
Spring throws MethodArgumentNotValidException
  ↓
GlobalExceptionHandler.handleValidation()
  ↓
HTTP 400: {
  "serviceName": "must not be blank"
}
```

#### Resource Not Found
```
GET /api/services/{unknown-id}
  ↓
ServiceCatDao.findById() returns null
  ↓
Service throws EntityNotFoundException
  ↓
GlobalExceptionHandler.handleEntityNotFound()
  ↓
HTTP 404: "Service not found"
```

#### Data Integrity Violations
```
POST /api/services with duplicate serviceName
  ↓
Database constraint violation
  ↓
Spring ORM throws DataIntegrityViolationException
  ↓
GlobalExceptionHandler.handleConstraint()
  ↓
HTTP 400: "Invalid request data"
```

### Missing Exception Handlers
⚠️ **Potential Issues:**
- NullPointerException → Not explicitly handled (HTTP 500)
- AccessDeniedException → Not explicitly handled (Spring Security default)
- Unhandled async exceptions (Kafka consumer failures)

---

## Rate Limiting Implementation

**Algorithm:** Token Bucket with per-user, per-endpoint granularity

**Location:** `RateLimitFilter.java` | `TokenBucket.java` | `RateLimitRegistry.java` | `RateLimitRule.java`

### How Token Bucket Works

```
Initial state: bucket = full (capacity tokens)

On each request:
1. Calculate tokens to refill since last refill
   tokens_to_add = (elapsed_time_ms * refill_rate) / 1000
2. Add tokens to bucket (max capacity)
3. If tokens > 0: consume 1 token, allow request
4. Else: reject request (HTTP 429)

Example:
  capacity = 100 tokens
  refill_rate = 50 tokens/second
  After 1 second: bucket = min(100, 100 - 1 + 50) = 100 full
  10 requests consume 10 tokens → 90 remaining
  After 2 seconds: bucket = min(100, 90 + 100) = 100 full
```

### Bucket Key Format

```
<ROLE>:<USERNAME>:<PATH_PREFIX>
```

**Example:** `ADMIN:john_doe:/api/metrics`

**Purpose:** Separate rate limits by role and endpoint

### Rate Limiting Rules (Configured)

| Endpoint | Path Prefix | Capacity (Burst) | Refill Rate | Max per minute |
|----------|------------|------------------|------------|-----------------|
| Admin API | `/api/admin` | 100 | 50 tokens/s | 3000 |
| Metrics Ingest | `/api/metrics/ingest` | 20 | 5 tokens/s | 300 |
| Logs Ingest | `/api/logs/ingest` | 30 | 10 tokens/s | 600 |
| Default | `/api` | 60 | 20 tokens/s | 1200 |

### Filtering Logic

```java
// In RateLimitFilter.doFilterInternal()

1. Extract request path
2. Find matching RateLimitRule (based on path prefix)
3. If rule not found: No rate limiting applied
4. If user not authenticated: Skip rate limiting (public endpoints)
5. Build bucket key: ROLE:USERNAME:PATH_PREFIX
6. Get or create TokenBucket from registry
7. Try to consume 1 token
8. If failed: Return HTTP 429 "Rate limit exceeded"
9. If succeeded: Continue to next filter
```

### Edge Cases

- **First request:** Bucket at capacity → Request succeeds
- **Burst requests:** Multiple requests rapidly consume tokens
- **After waiting:** Tokens refilled, more requests allowed
- **User role change:** New bucket key (old data discarded)
- **Service restart:** All buckets reset (no persistence)

### Production Recommendations

⚠️ **Current Issues:**
- Buckets stored in-memory (lost on restart)
- Per-server enforcement (distributed deployments need coordination)
- No persistent rate limit state

**Recommended:**
- Database-backed rate limiting for distributed systems
- Redis cache for high-performance rate limit checks
- Centralized policy configuration

---

## Audit & Logging System

**Location:** `AuditAspect.java` | `Auditable.java` | `AuditLog.java` | `AuditLogRepo.java`

### Decorator Pattern Implementation

```java
@Auditable(
    action = "UPDATE_SERVICE",
    resource = "SERVICE",
    resourceIdParam = "id"
)
public ServiceCatResponse update(UUID id, ServiceCatRequest request) {
    // Method logic
}
```

### Audit Flow

```
1. Method call (e.g., update())
   ↓
2. AuditAspect intercepts @Auditable annotation
   ↓
3. Method executes normally
   ↓
4a. SUCCESS PATH:
    @AfterReturning intercept
    ├─ Extract actor from SecurityContext
    ├─ Extract resourceId from method parameters
    ├─ Create AuditLog entry
    │  ├─ actor = current username
    │  ├─ action = UPDATE_SERVICE
    │  ├─ resource = SERVICE
    │  ├─ resourceId = method param 'id'
    │  ├─ outcome = SUCCESS
    │  ├─ error = null
    │  └─ timestamp = now
    └─ Save to audit_logs table
    
4b. FAILURE PATH:
    @AfterThrowing intercept
    ├─ Extract actor from SecurityContext
    ├─ Extract resourceId from method parameters
    ├─ Create AuditLog entry
    │  ├─ actor = current username
    │  ├─ action = UPDATE_SERVICE
    │  ├─ resource = SERVICE
    │  ├─ resourceId = method param 'id'
    │  ├─ outcome = FAILURE
    │  ├─ error = exception message
    │  └─ timestamp = now
    ├─ Save to audit_logs table
    └─ Exception re-thrown (not suppressed)
```

### Audited Operations

| Operation | Action | Resource | Params |
|-----------|--------|----------|--------|
| Register user (Auth) | REGISTER_USER | USER | - |
| Create service | CREATE_SERVICE | SERVICE | - |
| Update service | UPDATE_SERVICE | SERVICE | `id` |
| Delete service | DELETE_SERVICE | SERVICE | `id` |
| Deploy service | DEPLOY | SERVICE | `serviceId` |

### Audit Log Entity

```sql
CREATE TABLE audit_logs (
    audit_id UUID PRIMARY KEY,
    actor VARCHAR(255),              -- Username
    action VARCHAR(100),             -- UPDATE_SERVICE, DEPLOY, etc.
    resource VARCHAR(50),            -- SERVICE, USER, DEPLOYMENT
    resource_id VARCHAR(255),        -- Service UUID, User ID, etc.
    outcome VARCHAR(20),             -- SUCCESS, FAILURE
    error TEXT,                      -- Exception message if FAILURE
    timestamp TIMESTAMP
);
```

### Edge Cases

- **No authenticated user:** actor = null
- **Method parameter name mismatch:** resourceId = null
- **AuditLog save fails:** Exception propagated (transaction rolls back)
- **Async exceptions:** Not automatically audited (async code outside aspect scope)

### Logging Best Practices

✅ **Implemented:**
- Every data modification tracked
- Success and failure recorded
- Actor (who) and action (what) captured
- Timestamp for when

⚠️ **Missing:**
- IP address of requester
- Request/response body details
- Change tracking (before/after values)

---

## Edge Cases & Validation

### Input Validation

#### Request Body Validation
- Uses `@Valid` annotation with `@RequestBody`
- Spring's MethodArgumentNotValidException handles failures
- Field-level constraints (@NotNull, @NotBlank, etc.)

#### Edge Cases
```
1. Null Request Body
   → BadRequest (HTTP 400)

2. Extra JSON Fields
   → Ignored (Jackson default)

3. Type Mismatch (e.g., string for UUID)
   → HttpMessageNotReadableException
   → HTTP 400

4. Missing Required Fields
   → MethodArgumentNotValidException
   → HTTP 400 with field errors
```

### Authorization Validation

#### Role-Based (RBAC)
- `@PreAuthorize("hasRole('ADMIN')")`
- Spring Security evaluates at method entry
- Failures: AccessDeniedException → HTTP 403

#### Team-Based (TBAC)
- `OwnershipGuard.assertCanModify()`
- Called within service method
- Failures: AccessDeniedException → HTTP 403

### Data Integrity

#### Unique Constraints
- Service name uniqueness
- Username uniqueness
- Database-enforced constraints

#### Referential Integrity
- Service ID references validated implicitly
- Cascading deletes configured at DB level

### Time-Based Edge Cases

#### Future Timestamps
- Metric timestamps in future: Allowed
- Deployment dates in future: Allowed
- Potential issues: Skewed health status calculation

#### Null Timestamps
- Null createdAt: Allowed (should default to now)
- Null deployedAt: Allowed
- Null metric timestamp: Logged as null in response

### Resource Not Found

#### Behavior
```
1. Single resource by ID (e.g., GET /api/services/{id})
   → EntityNotFoundException
   → HTTP 404

2. Collection (e.g., GET /api/services/all)
   → Empty page/array returned
   → HTTP 200 (not 404)

3. Relationships (e.g., service → metrics)
   → No automatic FK validation at service layer
   → Orphaned records allowed
```

---

## Data Models

### Entity Relationships

```
Users (1) ─── (N) UserRoles
  │
  ├─ (N) RefreshTokens
  └─ (N) AuditLogs (actor)

Services (1) ─── (N) Deployments
       │
       ├─ (N) Metrics
       ├─ (N) Logs
       └─ (N) Alerts

Deployments (1) ─── (N) Rollbacks (implicit via status)
```

### Key Entities

#### UserEntity
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    team VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id UUID,
    role VARCHAR(50),
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

**Key Points:**
- Password stored as BCrypt hash
- Multiple roles per user (many-to-many)
- Team assignment for TBAC

#### ServiceCatInfo
```sql
CREATE TABLE services (
    service_id UUID PRIMARY KEY,
    service_name VARCHAR(255) UNIQUE NOT NULL,
    repo_url VARCHAR(500),
    owner_team VARCHAR(255),
    runtime VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP
);
```

**Key Points:**
- serviceName as business key (unique)
- ownerTeam for TBAC enforcement
- Status tracks service lifecycle (ACTIVE, MAINTENANCE, DEPRECATED)

#### MetricEntity
```sql
CREATE TABLE metrics (
    metric_id UUID PRIMARY KEY,
    service_id UUID NOT NULL,
    cpu_usage DECIMAL(5,2),
    memory_usage_mb INTEGER,
    timestamp TIMESTAMP NOT NULL,
    FOREIGN KEY (service_id) REFERENCES services(service_id)
);

CREATE INDEX idx_metrics_service_ts ON metrics(service_id, timestamp DESC);
```

**Key Points:**
- Composite index for efficient time-range queries
- Null metrics allowed (heartbeat case)

#### DeploymentEntity
```sql
CREATE TABLE deployments (
    deployment_id UUID PRIMARY KEY,
    service_id UUID NOT NULL,
    version VARCHAR(50),
    env VARCHAR(50),
    status VARCHAR(50),
    deployed_at TIMESTAMP,
    triggered_by VARCHAR(255),
    FOREIGN KEY (service_id) REFERENCES services(service_id)
);

CREATE INDEX idx_deployments_service ON deployments(service_id, deployed_at DESC);
```

**Key Points:**
- Deployment history (no updates, only inserts)
- Rollback recorded as separate deployment with status="ROLLED_BACK"

#### LogEntity
```sql
CREATE TABLE logs (
    log_id UUID PRIMARY KEY,
    service_id UUID NOT NULL,
    message TEXT,
    level VARCHAR(20),
    timestamp TIMESTAMP,
    source VARCHAR(255),
    FOREIGN KEY (service_id) REFERENCES services(service_id)
);

CREATE INDEX idx_logs_service_ts ON logs(service_id, timestamp DESC);
```

#### AlertEntity
```sql
CREATE TABLE alerts (
    alert_id UUID PRIMARY KEY,
    service_id UUID NOT NULL,
    type VARCHAR(50),
    severity VARCHAR(20),
    message VARCHAR(500),
    status VARCHAR(20),
    created_at TIMESTAMP,
    resolved_at TIMESTAMP,
    FOREIGN KEY (service_id) REFERENCES services(service_id)
);

CREATE INDEX idx_alerts_service_status ON alerts(service_id, status);
```

#### RefreshToken
```sql
CREATE TABLE refresh_tokens (
    token_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token VARCHAR(500) UNIQUE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP,
    expires_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

#### AuditLog
```sql
CREATE TABLE audit_logs (
    audit_id UUID PRIMARY KEY,
    actor VARCHAR(255),
    action VARCHAR(100),
    resource VARCHAR(50),
    resource_id VARCHAR(255),
    outcome VARCHAR(20),
    error TEXT,
    timestamp TIMESTAMP
);

CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp DESC);
```

---

## Async Processing Architecture

### AsyncIngestor Pattern

**Purpose:** Decouple request/response from time-consuming data processing

**Implementations:**
1. **KafkaIngestor:** Publish to Kafka topic
2. **SyncIngestor:** Synchronous execution (fallback)

### Flow

#### Synchronous Request Path (Fast Return)
```
POST /api/metrics/ingest/{serviceId}
  ├─ Validation
  ├─ AsyncIngestor.submit(metricRequest)
  │   ├─ If Kafka enabled:
  │   │   └─ KafkaMetricProducer.sendMessage(metricRequest)
  │   │       └─ Message queued (return immediately)
  │   └─ If Kafka disabled:
  │       └─ Execute synchronous: MetricServiceImpl.ingestInternal()
  └─ HTTP 202 Accepted (immediate)
```

#### Asynchronous Processing Path (Background)
```
Kafka Consumer Thread (or sync execution)
  ├─ KafkaMetricConsumer.onMessage(metricRequest)
  ├─ ServiceCatDao.findById(serviceId)
  ├─ MetricMapper.toEntity(request, service)
  ├─ MetricsDao.save(entity)
  └─ Log completion
```

### Kafka Configuration

**Topics:**
- `metric-topic`: Partitioned by serviceId (preserve order per service)
- `log-topic`: Partitioned by serviceId

**Benefits:**
- High throughput (multiple consumers)
- Fault tolerance (broker persistence)
- Order guarantee per partition

**Fallback:**
- If Kafka unavailable, switch to synchronous processing
- Enabled via Spring profiles (`kafka-enabled` profile)

### Error Handling in Async

**Consumer Failures:**
- Metrics/Logs ingest fails → Message discarded or DLQ (dead letter queue)
- No retry to client (202 already sent)
- Logged for monitoring

---

## API Summary Table

| # | Method | Endpoint | Auth | Rate | Async | Audit |
|---|--------|----------|------|------|-------|-------|
| 1 | POST | /auth/register | ❌ | Default | ❌ | ❌ |
| 2 | POST | /auth/login | ❌ | Default | ❌ | ❌ |
| 3 | POST | /auth/refresh | ❌ | Default | ❌ | ❌ |
| 4 | POST | /auth/logout | ❌ | Default | ❌ | ❌ |
| 5 | POST | /api/services | ✅ | Default | ❌ | ❌ |
| 6 | GET | /api/services | ✅ | Default | ❌ | ❌ |
| 7 | GET | /api/services/all | ✅ | Default | ❌ | ❌ |
| 8 | GET | /api/services/{id} | ✅ | Default | ❌ | ❌ |
| 9 | PUT | /api/services/{id} | ✅ | Default | ❌ | ✅ |
| 10 | DELETE | /api/services/{id} | ✅ | Default | ❌ | ✅ |
| 11 | POST | /api/deployments | ✅ ADMIN | Default | ❌ | ✅ |
| 12 | GET | /api/deployments/{svc} | ✅ A/V | Default | ❌ | ❌ |
| 13 | GET | /api/deployments/{svc}/latest | ✅ A/V | Default | ❌ | ❌ |
| 14 | GET | /api/deployments/{svc}/summary | ✅ A/V | Default | ❌ | ❌ |
| 15 | POST | /api/deployments/rollback | ✅ ADMIN | Default | ❌ | ❌ |
| 16 | POST | /api/metrics/ingest/{svc} | ✅ ADMIN | Strict | ✅ | ❌ |
| 17 | POST | /api/metrics/heartbeat/{svc} | ✅ ADMIN | Default | ❌ | ❌ |
| 18 | GET | /api/metrics/{svc}/latest | ✅ A/V | Default | ❌ | ❌ |
| 19 | GET | /api/metrics/{svc} | ✅ Any | Default | ❌ | ❌ |
| 20 | GET | /api/metrics/{svc}/health | ❌ | Default | ❌ | ❌ |
| 21 | GET | /api/metrics/{svc}/summary | ✅ A/V | Default | ❌ | ❌ |
| 22 | POST | /api/logs/ingest/{svc} | ✅ ADMIN | Strict | ✅ | ❌ |
| 23 | GET | /api/logs/{svc} | ✅ A/V | Default | ❌ | ❌ |
| 24 | POST | /api/alerts/evaluate/{svc} | ✅ ADMIN | Default | ❌ | ❌ |
| 25 | GET | /api/alerts | ✅ A/V | Default | ❌ | ❌ |
| 26 | GET | /api/alerts/{svc} | ✅ A/V | Default | ❌ | ❌ |
| 27 | POST | /api/alerts/{id}/resolve | ✅ A/V | Default | ❌ | ❌ |

**Auth:** ✅ = Required | ❌ = Not Required | ADMIN = Admin only | A/V = Admin or Viewer  
**Rate:** Default = 20 tokens/s | Strict = Lower limits for bulk ingest  
**Async:** ✅ = Async processing | ❌ = Synchronous  
**Audit:** ✅ = Logged | ❌ = Not logged

---

## Security Considerations

### Best Practices Implemented ✅
1. Password hashing (BCrypt)
2. JWT signed tokens (HMAC-SHA256)
3. Role-based access control
4. Team-based access control
5. Token revocation (refresh tokens)
6. Request validation
7. Audit logging

### Recommendations for Production 🔧
1. **HTTPS enforcement** (TLS 1.3+)
2. **JWT secret externalization** (environment variables, vault)
3. **API key rotation** (change JWT secret periodically)
4. **Request signing** (for sensitive operations)
5. **CORS configuration** (if frontend on different domain)
6. **Rate limiting persistence** (Redis or database)
7. **DDoS protection** (WAF, load balancer)
8. **Log aggregation** (ELK, CloudWatch)
9. **Intrusion detection** (IDS/IPS)
10. **Penetration testing** (security audit)

---

## Performance Considerations

### Indexing Strategy
✅ **Implemented:**
- Service name unique index
- Metrics service+timestamp composite index
- Deployments service+timestamp composite index
- Logs service+timestamp composite index

### Query Optimization
✅ **Good Patterns:**
- Pagination for large result sets
- Filtered queries (don't fetch all then filter)
- Bulk operations (batch inserts)

⚠️ **Potential Issues:**
- GET /api/services (no pagination) → All services fetched
- Summary aggregation on large datasets → Slow queries
- No query result caching

### Async Benefits
✅ **Implemented:**
- Metrics ingest async (Kafka or queue)
- Logs ingest async
- Non-blocking request handling

### Bottlenecks
⚠️ **Identified:**
- Single database connection pool
- No caching (every request hits DB)
- No read replicas for queries

---

## Conclusion

The IDP Backend implements a **comprehensive microservice architecture** with:
- **6 Controllers** managing 27 API endpoints
- **JWT-based authentication** with role and team-based authorization
- **Token bucket rate limiting** with per-user, per-endpoint granularity
- **Async processing** via Kafka for high-volume ingest operations
- **Audit logging** via AspectJ decorators for data mutations
- **Centralized exception handling** with meaningful HTTP status codes
- **Dynamic query building** for flexible filtering and pagination

The system balances **functionality, security, and performance** with thoughtful design patterns and comprehensive error handling. Production deployment should address the recommendations around security hardening, distributed rate limiting, and query optimization.

---

**Document Version:** 1.0  
**Last Updated:** April 25, 2026  
**Analysis Depth:** Comprehensive (All 27 endpoints detailed)
