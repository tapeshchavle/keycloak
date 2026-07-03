# 🔐 Enterprise IAM — Keycloak + OpenLDAP + SSO

**Production-grade Identity & Access Management** with:
- **Keycloak 25** — OIDC/OAuth2 Authorization Server
- **OpenLDAP** — Enterprise User Directory
- **Spring Boot 3.3** — JWT-protected microservices
- **PostgreSQL 16** — Keycloak persistence

---

## 🚀 Quick Start

```bash
cd docker

# 1. Copy environment template
cp .env.example .env

# 2. Start main containers
docker compose up -d

# Starts apps profile containers
docker-compose --profile apps up -d


# 3. Wait for Keycloak to be ready (~90 seconds)
docker compose logs -f keycloak

# 4. Verify services
curl http://localhost:8080/health/ready    # Keycloak
curl http://localhost:8081/actuator/health # Main App
curl http://localhost:8082/actuator/health # Payroll
curl http://localhost:8083/actuator/health # HR
curl http://localhost:8084/actuator/health # Inventory
```

---

## 🌐 Service URLs

| Service | URL | Description |
|---|---|---|
| Keycloak Admin | http://localhost:8080/admin | admin / Admin@Secure2024 |
| phpLDAPadmin | http://localhost:8090 | LDAP browser UI |
| Main API Gateway | http://localhost:8081 | Auth + user management |
| Payroll Service | http://localhost:8082 | Salary management |
| HR Service | http://localhost:8083 | Employee management |
| Inventory Service | http://localhost:8084 | Product management |
| Swagger (Main) | http://localhost:8081/swagger-ui.html | API docs |
| Swagger (Payroll) | http://localhost:8082/swagger-ui.html | Payroll API docs |

---

## 👥 Test Users (from LDAP)

| Username | Password | Roles | Can Access |
|---|---|---|---|
| `admin.user` | `Test@1234` | ADMIN+USER+PAYROLL+HR+INVENTORY | Everything |
| `john.doe` | `Test@1234` | USER+PAYROLL | Own salary + Payroll data |
| `jane.smith` | `Test@1234` | USER+HR | Own profile + HR data |
| `bob.johnson` | `Test@1234` | USER+INVENTORY | Products + Stock |
| `alice.chen` | `Test@1234` | USER only | Own profile + Product catalog |

---

## 🧪 API Testing Guide — Step by Step

### STEP 1: Login → Get JWT Token

```bash
# Login as john.doe (ROLE_USER + ROLE_PAYROLL)
curl -X POST http://localhost:8081/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "john.doe", "password": "Test@1234"}'

# Response:
# {
#   "access_token": "eyJhbGci...",   ← Use this for API calls
#   "expires_in": 300,               ← Valid for 5 minutes
#   "refresh_token": "eyJhbGci...",  ← Use this to get new access_token
#   "token_type": "Bearer"
# }
```

### STEP 2: Use JWT Token for API Calls

```bash
# Save the token
TOKEN="eyJhbGci..."  # paste your access_token here

# Get current user info (decoded from JWT — no DB call!)
curl http://localhost:8081/api/auth/me \
  -H "Authorization: Bearer $TOKEN"

# Get your salary (john.doe can see his own salary)
curl http://localhost:8082/api/payroll/my-salary \
  -H "Authorization: Bearer $TOKEN"

# Get ALL salaries (john.doe has ROLE_PAYROLL — allowed!)
curl http://localhost:8082/api/payroll/salaries \
  -H "Authorization: Bearer $TOKEN"

# Try HR data (john.doe has NO ROLE_HR — 403 Forbidden!)
curl http://localhost:8083/api/hr/employees \
  -H "Authorization: Bearer $TOKEN"
# → {"error":"FORBIDDEN","message":"You don't have the required role..."}

# View products (any USER can browse catalog)
curl http://localhost:8084/api/inventory/products \
  -H "Authorization: Bearer $TOKEN"
```

### STEP 3: Test with Different Users (SSO Demo)

```bash
# Login as alice.chen (ROLE_USER only)
curl -X POST http://localhost:8081/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "alice.chen", "password": "Test@1234"}'

ALICE_TOKEN="..."

# alice.chen can view product catalog (ROLE_USER allowed)
curl http://localhost:8084/api/inventory/products \
  -H "Authorization: Bearer $ALICE_TOKEN"  # ✅ 200 OK

# alice.chen CANNOT see stock levels (needs ROLE_INVENTORY)
curl http://localhost:8084/api/inventory/stock \
  -H "Authorization: Bearer $ALICE_TOKEN"  # ❌ 403 Forbidden

# alice.chen CANNOT process payroll (needs ROLE_ADMIN)
curl -X POST http://localhost:8082/api/payroll/process \
  -H "Authorization: Bearer $ALICE_TOKEN"  # ❌ 403 Forbidden
```

### STEP 4: Admin — Full Access

```bash
# Login as admin.user (ALL ROLES)
curl -X POST http://localhost:8081/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "admin.user", "password": "Test@1234"}'

ADMIN_TOKEN="..."

# Admin can do everything:
curl http://localhost:8082/api/payroll/salaries \
  -H "Authorization: Bearer $ADMIN_TOKEN"        # ✅ All salaries

curl -X POST http://localhost:8082/api/payroll/process \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"period": "2024-12"}'                     # ✅ Process payroll

curl http://localhost:8083/api/hr/employees \
  -H "Authorization: Bearer $ADMIN_TOKEN"        # ✅ All employees

curl -X DELETE http://localhost:8084/api/inventory/products/PROD001 \
  -H "Authorization: Bearer $ADMIN_TOKEN"        # ✅ Delete product
```

### STEP 5: Refresh Token (When Access Token Expires in 5 Min)

```bash
curl -X POST http://localhost:8081/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refresh_token": "eyJhbGci..."}'

# Returns: new access_token (valid 5 min) + new refresh_token
```

### STEP 6: Logout

```bash
curl -X POST http://localhost:8081/api/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"refresh_token": "eyJhbGci..."}'

# Keycloak revokes the session. refresh_token is now invalid.
# access_token remains valid until it expires (max 5 min).
```

### STEP 7: Debug — See Raw JWT Claims

```bash
# See exactly what claims are in your JWT (Keycloak signed)
curl http://localhost:8081/api/auth/validate \
  -H "Authorization: Bearer $TOKEN"

# See what payroll service sees in the JWT
curl http://localhost:8082/api/payroll/jwt-debug \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🔍 How JWT Validation Works Internally

```
Request: GET /api/payroll/salaries
Headers: Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Ii4uLiJ9...

Payroll Service (internal flow):
1. JwtAuthenticationFilter intercepts request
2. Extracts token from Authorization header
3. Downloads public key from Keycloak JWKS (cached at startup):
   GET http://keycloak:8080/realms/company/protocol/openid-connect/certs
4. Verifies RSA signature: RSA_VERIFY(header.payload, signature, publicKey) → VALID
5. Checks: exp = 1751360400 > now → NOT EXPIRED
6. Checks: iss = "http://keycloak:8080/realms/company" → CORRECT ISSUER
7. KeycloakJwtConverter extracts: realm_access.roles = ["PAYROLL", "USER"]
8. Creates: [ROLE_PAYROLL, ROLE_USER] as Spring GrantedAuthority
9. @PreAuthorize("hasAnyRole('PAYROLL', 'ADMIN')") → ROLE_PAYROLL found → ALLOW
10. Controller executes → Response returned

Total Keycloak calls for this request: 0 (public key was cached at startup)
Total LDAP calls for this request: 0
```

---

## 🏗️ Project Structure

```
keyclock/
├── docker/
│   ├── docker-compose.yml           ← Full stack orchestration
│   ├── .env.example                 ← Environment template
│   ├── openldap/bootstrap/          ← LDAP directory structure
│   │   ├── 00-base.ldif             ← OUs: People, Groups, ServiceAccounts
│   │   ├── 01-groups.ldif           ← Enterprise groups → Keycloak roles
│   │   └── 02-users.ldif            ← Sample users (dev only)
│   ├── keycloak/realm-export/
│   │   └── company-realm.json       ← Realm as code (auto-imported at startup)
│   └── init-scripts/
│       └── init-db.sql
├── services/
│   ├── payroll-service/             ← Port 8082: ROLE_PAYROLL protected
│   ├── hr-service/                  ← Port 8083: ROLE_HR protected
│   └── inventory-service/           ← Port 8084: ROLE_INVENTORY protected
├── src/                             ← Main API Gateway (Port 8081)
│   └── main/java/keyclock/example/
│       ├── config/SecurityConfig.java        ← JWT resource server setup
│       ├── config/KeycloakJwtConverter.java  ← Role extraction from JWT
│       ├── controller/AuthController.java    ← Login, refresh, logout
│       ├── controller/UserController.java    ← User management
│       ├── dto/TokenRequest|Response.java
│       └── service/KeycloakTokenService.java ← Keycloak token proxy
├── Dockerfile
└── pom.xml
```

---

## 🔐 Security Architecture

```
OpenLDAP (389/636)
  ↕ LDAP Federation (Keycloak reads users/groups)
Keycloak (8080) — Issues JWT tokens signed with RS256 private key
  ↕ JWT Bearer tokens (Authorization: Bearer eyJhbGci...)
┌────────────────────────────────────────────┐
│  All services validate JWT LOCALLY using   │
│  Keycloak's PUBLIC KEY (downloaded once)   │
│  No per-request calls to Keycloak or LDAP  │
├──────────────┬───────────────┬─────────────┤
│ Main Gateway │ Payroll :8082 │ HR    :8083 │ Inventory :8084
│      :8081   │ ROLE_PAYROLL  │ ROLE_HR     │ ROLE_INVENTORY
└──────────────┴───────────────┴─────────────┘
```

# keycloak
