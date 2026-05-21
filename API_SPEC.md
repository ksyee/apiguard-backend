# APIGuard Backend API Specification

APIGuard는 외부 API 의존성이 있는 개발팀을 위한 **API Reliability & Contract Change Detection SaaS**입니다.
이 API는 외부 API 장애/응답 지연 감지, 연속 실패 기반 Incident 관리, Redis cooldown 기반 알림 제어, OpenAPI snapshot 비교를 통한 breaking change 감지를 지원합니다.

## 1) Common

### Base URL
- Local: `http://localhost:8080`

### Authentication
- Scheme: Bearer JWT
- Header: `Authorization: Bearer {accessToken}`
- Access/Refresh token issuance: `POST /auth/login`
- Token refresh: `POST /auth/refresh`

### Public (No Auth) Endpoints
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `POST /users/signup`
- `GET /health`
- `GET /status/{slug}`
- `GET /error`
- `GET /swagger-ui/**` (dev profile)
- `GET /v3/api-docs/**` (dev profile)

### Response Envelope
All endpoints return `ApiResponse<T>`.

```json
{
  "success": true,
  "data": {}
}
```

```json
{
  "success": false,
  "message": "error message"
}
```

### Error Status Mapping
- `400`: validation error, bad request, JSON parse error
- `401`: unauthorized / invalid credentials
- `402`: plan limit exceeded
- `403`: forbidden
- `404`: not found
- `409`: duplicate email
- `502`: payment provider error
- `500`: internal server error

### Enums
- `HttpMethod`: `GET | POST | PUT | PATCH | DELETE | HEAD | OPTIONS`
- `CheckStatus`: `SUCCESS | FAILURE | TIMEOUT | ERROR`
- `AlertType`: `EMAIL | SLACK | WEBHOOK`
- `AlertDeliveryStatus`: `SUCCESS | FAILED`
- `IncidentStatus`: `OPEN | RESOLVED`
- `IncidentType`: `AVAILABILITY | PERFORMANCE | CONTRACT_CHANGE`
- `BreakingChangeRule`: `PATH_REMOVED | METHOD_REMOVED | REQUIRED_PARAMETER_ADDED | REQUIRED_REQUEST_BODY_ADDED | REQUEST_BODY_REQUIRED_FIELD_ADDED | RESPONSE_FIELD_REMOVED | RESPONSE_FIELD_TYPE_CHANGED`
- `WorkspaceRole`: `OWNER | ADMIN | MEMBER | VIEWER`
- `PaymentStatus`: `PENDING | SUCCESS | FAILED | CANCELLED`

---

## 2) Auth

### 2.1 Login
`POST /auth/login`

Request
```json
{
  "email": "user@example.com",
  "password": "Password1!"
}
```

Response `200`
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ..."
  }
}
```

### 2.2 Refresh
`POST /auth/refresh`

Request
```json
{
  "refreshToken": "eyJ..."
}
```

Response `200`
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ..."
  }
}
```

### 2.3 Logout
`POST /auth/logout`

Request
```json
{
  "refreshToken": "eyJ..."
}
```

Response `200`
```json
{ "success": true }
```

---

## 3) User

### 3.1 Sign Up
`POST /users/signup`

Request
```json
{
  "email": "user@example.com",
  "password": "Password1!",
  "nickname": "홍길동"
}
```

Response `200`
```json
{
  "success": true,
  "data": 1
}
```

Notes
- On sign-up, personal workspace + FREE subscription are auto-created.

### 3.2 Get Me
`GET /users/me` (auth required)

Response `200`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "nickname": "홍길동",
    "createdAt": "2026-02-26T00:00:00"
  }
}
```

### 3.3 Update Me
`PATCH /users/me` (auth required)

Request
```json
{
  "nickname": "새닉네임"
}
```

Response `200`
```json
{ "success": true }
```

### 3.4 Change Password
`PATCH /users/me/password` (auth required)

Request
```json
{
  "currentPassword": "OldPassword1!",
  "newPassword": "NewPassword1!",
  "newPasswordConfirm": "NewPassword1!"
}
```

Response `200`
```json
{ "success": true }
```

### 3.5 Delete Me
`DELETE /users/me` (auth required)

Response `200`
```json
{ "success": true }
```

---

## 4) Workspace

### 4.1 Create Workspace
`POST /workspaces` (auth required)

Request
```json
{
  "name": "팀 워크스페이스"
}
```

Response `200`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "팀 워크스페이스",
    "slug": "팀-워크스페이스-1",
    "role": "OWNER",
    "createdAt": "2026-02-26T00:00:00"
  }
}
```

### 4.2 List My Workspaces
`GET /workspaces` (auth required)

Response `200`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "팀 워크스페이스",
      "slug": "팀-워크스페이스-1",
      "role": "OWNER",
      "createdAt": "2026-02-26T00:00:00"
    }
  ]
}
```

### 4.3 Get Workspace
`GET /workspaces/{id}` (auth required, workspace member)

### 4.4 Delete Workspace
`DELETE /workspaces/{id}` (auth required, `OWNER` only)

### 4.5 List Members
`GET /workspaces/{id}/members` (auth required, workspace member)

Response item
```json
{
  "userId": 2,
  "nickname": "김철수",
  "email": "invite@example.com",
  "role": "MEMBER",
  "joinedAt": "2026-02-26T00:00:00"
}
```

### 4.6 Invite Member
`POST /workspaces/{id}/members` (auth required, `ADMIN` or `OWNER`)

Request
```json
{
  "email": "invite@example.com",
  "role": "MEMBER"
}
```

Notes
- Invite target must already exist user account.
- FREE plan cannot invite members (`402`).
- `role` defaults to `MEMBER` when omitted.
- `OWNER` cannot be assigned by invitation.
- `ADMIN` can invite only `MEMBER` or `VIEWER`; only `OWNER` can invite `ADMIN`.

### 4.7 Update Member Role
`PATCH /workspaces/{id}/members/{userId}/role` (auth required, `OWNER` only)

Request
```json
{
  "role": "ADMIN"
}
```

### 4.8 Remove Member
`DELETE /workspaces/{id}/members/{userId}` (auth required, `OWNER` only)

---

## 5) Subscription & Payment

Base path: `/api/workspaces/{workspaceId}`

### 5.1 Get Subscription Status
`GET /api/workspaces/{workspaceId}/subscription` (auth required, workspace member)

Response `200`
```json
{
  "success": true,
  "data": {
    "planType": "FREE",
    "active": true,
    "cancelAtPeriodEnd": false,
    "expiredAt": null,
    "maxProjects": 3,
    "maxEndpointsPerProject": 5,
    "minCheckIntervalSeconds": 300,
    "maxAlertChannels": 1,
    "maxMembers": 1,
    "dataRetentionDays": 7
  }
}
```

Note
- Unlimited values are represented as `-1` (e.g. PRO `maxMembers`, `maxAlertChannels`).

### 5.2 Prepare Payment
`POST /api/workspaces/{workspaceId}/payment/prepare` (auth required, `OWNER` only)

Response `200`
```json
{
  "success": true,
  "data": {
    "orderId": "apiguard-1-a1b2c3d4e5f6",
    "amount": 19900,
    "orderName": "ApiGuard PRO 플랜 (1개월)",
    "clientKey": "test_ck_...",
    "customerKey": "apiguard_xxx",
    "customerEmail": "owner@example.com",
    "customerName": "Owner"
  }
}
```

### 5.3 Confirm Payment
`POST /api/workspaces/{workspaceId}/payment/confirm` (auth required, `OWNER` only)

Request
```json
{
  "paymentKey": "tosspayments_key",
  "orderId": "apiguard-1-a1b2c3d4e5f6",
  "amount": 19900
}
```

### 5.4 Payment History
`GET /api/workspaces/{workspaceId}/payment/history` (auth required, workspace member)

### 5.5 Cancel Subscription
`POST /api/workspaces/{workspaceId}/subscription/cancel` (auth required, `OWNER` only)

Behavior
- Schedules the active PRO subscription to cancel at the current period end.
- PRO limits remain available until `expiredAt`; after the period expires, the workspace falls back to FREE.
- If there is no active PRO subscription, returns `409`.

Response `200`
```json
{
  "success": true,
  "data": {
    "planType": "PRO",
    "active": true,
    "cancelAtPeriodEnd": true,
    "expiredAt": "2026-06-20T10:00:00",
    "maxProjects": 50,
    "maxEndpointsPerProject": 50,
    "minCheckIntervalSeconds": 30,
    "maxAlertChannels": -1,
    "maxMembers": -1,
    "dataRetentionDays": 90
  }
}
```

---

## 6) Project

### 6.1 Create Project
`POST /workspaces/{workspaceId}/projects` (auth required, workspace write permission; `VIEWER` denied)

Request
```json
{
  "name": "프로덕션",
  "description": "메인 서비스"
}
```

Behavior
- Plan limit enforced: FREE 3 projects per workspace, PRO 50 projects per workspace.

### 6.2 List Projects in Workspace
`GET /workspaces/{workspaceId}/projects` (auth required, workspace member)

### 6.3 Get Project
`GET /projects/{id}` (auth required, workspace member or personal project owner)

### 6.4 Update Project
`PATCH /projects/{id}` (auth required, workspace `MEMBER` or above; personal project owner)

### 6.5 Delete Project
`DELETE /projects/{id}` (auth required, workspace `ADMIN` or above; personal project owner)

---

## 7) Endpoint

### 7.1 Create Endpoint
`POST /projects/{projectId}/endpoints` (auth required, workspace `MEMBER` or above; personal project owner)

Request
```json
{
  "url": "https://api.example.com/health",
  "httpMethod": "GET",
  "headers": { "Authorization": "Bearer token" },
  "body": null,
  "expectedStatusCode": 200,
  "checkInterval": 300
}
```

Behavior
- `expectedStatusCode` default: `200`
- `checkInterval` default: `60`
- Plan limits enforced on workspace projects:
  - endpoint count: FREE 5 / PRO 50
  - min check interval: FREE 300s / PRO 60s

### 7.2 List Endpoints
`GET /projects/{projectId}/endpoints` (auth required, workspace member or personal project owner)

### 7.3 Get Endpoint
`GET /endpoints/{id}` (auth required, workspace member or personal project owner)

### 7.4 Update Endpoint
`PUT /endpoints/{id}` (auth required, workspace `MEMBER` or above; personal project owner)

### 7.5 Delete Endpoint
`DELETE /endpoints/{id}` (auth required, workspace `ADMIN` or above; personal project owner)

### 7.6 Toggle Endpoint
`PATCH /endpoints/{id}/toggle` (auth required, workspace `MEMBER` or above; personal project owner)

Endpoint response model
```json
{
  "id": 1,
  "projectId": 1,
  "url": "https://api.example.com/health",
  "httpMethod": "GET",
  "headers": { "Authorization": "Bearer token" },
  "body": null,
  "expectedStatusCode": 200,
  "checkInterval": 300,
  "isActive": true,
  "lastCheckedAt": null,
  "createdAt": "2026-02-26T00:00:00"
}
```

---

## 8) Alert

### 8.1 Create Alert
`POST /endpoints/{endpointId}/alerts` (auth required, workspace `MEMBER` or above; personal project owner)

Request
```json
{
  "alertType": "EMAIL",
  "target": "alert@example.com",
  "threshold": 3
}
```

Behavior
- `threshold` default: `3`
- Plan limit enforced: FREE max 1 channel per endpoint, PRO unlimited
- Supported `alertType`: `EMAIL`, `SLACK`, `WEBHOOK`
- Duplicate successful alerts are suppressed by Redis cooldown (30 minutes)
- Delivery success/failure is persisted for audit and troubleshooting.

### 8.2 List Alerts
`GET /endpoints/{endpointId}/alerts` (auth required, workspace member or personal project owner)

### 8.3 Update Alert
`PUT /alerts/{id}` (auth required, workspace `MEMBER` or above; personal project owner)

### 8.4 Delete Alert
`DELETE /alerts/{id}` (auth required, workspace `MEMBER` or above; personal project owner)

### 8.5 Toggle Alert
`PATCH /alerts/{id}/toggle` (auth required, workspace `MEMBER` or above; personal project owner)

### 8.6 Send Test Alert
`POST /alerts/{id}/test` (auth required, workspace `MEMBER` or above; personal project owner)

Behavior
- Sends one test notification to the alert target.
- Stores the result as an alert delivery with `testDelivery=true`.
- Test delivery does not set the Redis cooldown key.

### 8.7 Alert Delivery History
`GET /alerts/{id}/deliveries` (auth required, workspace member or personal project owner)

Query params
- `limit` (default `20`, max `100`)

Alert response model
```json
{
  "id": 1,
  "endpointId": 1,
  "alertType": "EMAIL",
  "target": "alert@example.com",
  "threshold": 3,
  "isActive": true,
  "createdAt": "2026-02-26T00:00:00"
}
```

Alert delivery response model
```json
{
  "id": 10,
  "alertId": 1,
  "endpointId": 1,
  "alertType": "WEBHOOK",
  "target": "https://hooks.example.com/apiguard",
  "status": "SUCCESS",
  "testDelivery": true,
  "errorMessage": null,
  "triggeredAt": "2026-05-20T10:00:00"
}
```

---

## 9) Check & Stats

### 9.1 Manual Test Endpoint
`POST /endpoints/{id}/test` (auth required, workspace `MEMBER` or above; personal project owner)

### 9.2 Endpoint Stats (24h)
`GET /endpoints/{id}/stats` (auth required, workspace member or personal project owner)

Response model
```json
{
  "totalChecks": 1440,
  "successCount": 1435,
  "successRate": 99.65,
  "avgResponseTimeMs": 135.2,
  "since": "2026-02-25T00:00:00"
}
```

### 9.3 Hourly Stats (24h)
`GET /endpoints/{id}/stats/hourly` (auth required, workspace member or personal project owner)

### 9.4 Recent Checks
`GET /endpoints/{id}/checks` (auth required, workspace member or personal project owner)

Query params
- `limit` (default `20`)

### 9.5 Project Stats
`GET /projects/{id}/stats` (auth required, workspace member or personal project owner)

Response model
```json
{
  "totalEndpoints": 5,
  "upCount": 4,
  "downCount": 1,
  "avgResponseTimeMs": 150.3
}
```

---

## 10) Incidents

### 10.1 Project Incidents
`GET /projects/{projectId}/incidents` (auth required)

Query params
- `status`: optional `OPEN | RESOLVED`

### 10.2 Endpoint Incidents
`GET /endpoints/{endpointId}/incidents` (auth required)

Behavior
- 3 consecutive failed checks open an `AVAILABILITY` incident.
- A successful check resolves an open `AVAILABILITY` incident.
- 3 consecutive successful but slow responses over 1000ms open a `PERFORMANCE` incident.
- OpenAPI breaking changes open a project-level `CONTRACT_CHANGE` incident.

Response model
```json
{
  "id": 1,
  "endpointId": 1,
  "projectId": 1,
  "endpointUrl": "https://api.example.com/health",
  "type": "AVAILABILITY",
  "status": "OPEN",
  "severity": "CRITICAL",
  "title": "Endpoint availability incident",
  "description": "최근 3회 연속 상태 체크가 실패했습니다.",
  "detectedCount": 3,
  "startedAt": "2026-05-13T10:00:00",
  "lastDetectedAt": "2026-05-13T10:02:00",
  "resolvedAt": null
}
```

For `CONTRACT_CHANGE` incidents, `endpointId` and `endpointUrl` are `null` because the event belongs to the OpenAPI spec source and project, not to a single endpoint.

---

## 11) OpenAPI Spec Changes

### 11.1 Create Spec Source
`POST /projects/{projectId}/spec-sources` (auth required, workspace `MEMBER` or above; personal project owner)

Request
```json
{
  "name": "Payments API",
  "specUrl": "https://api.example.com/openapi.json"
}
```

### 11.2 List Spec Sources
`GET /projects/{projectId}/spec-sources` (auth required, workspace member or personal project owner)

### 11.3 Update Spec Source
`PUT /spec-sources/{sourceId}` (auth required, workspace `MEMBER` or above; personal project owner)

Request
```json
{
  "name": "Payments API v2",
  "specUrl": "https://api.example.com/openapi-v2.json",
  "active": true
}
```

Notes
- All fields are optional; only provided values are updated.
- `active=false` keeps the source but blocks manual checks until re-enabled.

### 11.4 Delete Spec Source
`DELETE /spec-sources/{sourceId}` (auth required, workspace `MEMBER` or above; personal project owner)

### 11.5 Toggle Spec Source
`PATCH /spec-sources/{sourceId}/toggle` (auth required, workspace `MEMBER` or above; personal project owner)

### 11.6 Check Spec Source
`POST /spec-sources/{sourceId}/check` (auth required, workspace `MEMBER` or above; personal project owner)

Behavior
- Fetches the current OpenAPI JSON.
- Stores a snapshot when the content hash changes.
- Compares the latest snapshot with the previous snapshot.
- Detects breaking changes for removed paths, removed methods, added required request parameters, newly required request bodies, added required request body fields, removed response fields, and changed response field types.
- Creates or updates an open `CONTRACT_CHANGE` incident when breaking changes are detected.
- Inactive spec sources return a bad request and are not checked.

Automatic checks
- Active, non-deleted spec sources are also checked by the scheduler.
- Deleted projects/workspaces and inactive sources are skipped.
- The default scheduler interval is `apiguard.apispec.check-fixed-delay-ms=300000` and can be disabled with `apiguard.apispec.auto-check-enabled=false`.

Outbound URL guard
- Health checks, OpenAPI spec fetches, and Slack/Webhook notifications only allow `http`/`https` URLs.
- Private network, loopback, link-local, multicast, and cloud metadata addresses are blocked by default.
- Local dev/test can opt in with `apiguard.outbound.allow-private-network=true`.

### 11.7 List Diffs
`GET /spec-sources/{sourceId}/diffs` (auth required, workspace member or personal project owner)

### 11.8 Diff Detail
`GET /spec-diffs/{diffId}` (auth required, workspace member or personal project owner)

Spec source response model
```json
{
  "id": 1,
  "projectId": 1,
  "name": "Payments API",
  "specUrl": "https://api.example.com/openapi.json",
  "active": true,
  "lastCheckedAt": "2026-05-20T10:00:00",
  "createdAt": "2026-05-13T10:00:00"
}
```

Response model
```json
{
  "id": 1,
  "specSourceId": 1,
  "baseSnapshotId": 1,
  "headSnapshotId": 2,
  "breaking": true,
  "breakingChangeCount": 2,
  "summary": "Detected 2 breaking change(s).",
  "checkedAt": "2026-05-13T10:00:00",
  "changes": [
    {
      "id": 1,
      "rule": "PATH_REMOVED",
      "location": "/v1/orders",
      "description": "기존 path가 삭제되었습니다."
    }
  ]
}
```

---

## 12) Status Page

### 12.1 Public Status Page
`GET /status/{slug}` (public)

Behavior
- Returns only public status pages.
- If `allEndpoints=true`, all active workspace endpoints are included.
- If `allEndpoints=false`, only active endpoints listed in `endpointIds` are exposed.
- If `allEndpoints=false` and `endpointIds=[]`, the public page is returned with no endpoint rows.
- For backward compatibility, omitting both `allEndpoints` and `endpointIds` on create publishes all active endpoints.

Response model
```json
{
  "title": "APIGuard Status",
  "description": "External API dependency status",
  "overallStatus": "OPERATIONAL",
  "endpoints": [
    {
      "url": "https://api.example.com/health",
      "httpMethod": "GET",
      "status": "UP",
      "uptimePercent": 99.9,
      "avgResponseTimeMs": 120.5,
      "lastCheckedAt": "2026-05-20T10:00:00"
    }
  ]
}
```

### 12.2 Create Status Page
`POST /workspaces/{workspaceId}/status-page` (auth required, workspace `MEMBER` or above)

Request
```json
{
  "title": "APIGuard Status",
  "description": "External API dependency status",
  "slug": "apiguard-status",
  "allEndpoints": false,
  "endpointIds": [1, 2]
}
```

Response status: `201`

### 12.3 Get Status Page
`GET /workspaces/{workspaceId}/status-page` (auth required, workspace member)

### 12.4 Update Status Page
`PUT /workspaces/{workspaceId}/status-page` (auth required, workspace `MEMBER` or above)

Request
```json
{
  "title": "APIGuard Public Status",
  "description": "Selected external API status",
  "isPublic": true,
  "allEndpoints": false,
  "endpointIds": [1]
}
```

Notes
- `endpointIds` must belong to the workspace.
- `allEndpoints=true` clears the explicit allowlist and exposes all active workspace endpoints.
- `allEndpoints=false` with `endpointIds=[]` exposes no endpoints.
- Omitting both `allEndpoints` and `endpointIds` during update keeps the existing endpoint selection.

### 12.5 Delete Status Page
`DELETE /workspaces/{workspaceId}/status-page` (auth required, workspace `MEMBER` or above)

Status page response model
```json
{
  "id": 1,
  "slug": "apiguard-status",
  "title": "APIGuard Status",
  "description": "External API dependency status",
  "isPublic": true,
  "createdAt": "2026-05-20T10:00:00",
  "allEndpoints": false,
  "endpointIds": [1, 2]
}
```

---

## 13) Health

### 13.1 Health Check
`GET /health` (public)

Response `200`
```json
{
  "success": true,
  "data": "APIGuard 서버가 정상적으로 구동중입니다."
}
```

### 13.2 Test Error
`GET /test-error` (auth required)

Behavior
- Intentionally throws runtime exception for error handling test.

---

## 14) Operational Notes

### Scheduled Jobs
- Health checks: fixed delay 60s scheduler + per-endpoint interval evaluation
- Retention cleanup: every day at `03:00` server time (`cron: 0 0 3 * * *`)

### Schema Notes
- `alert_deliveries` stores notification delivery success/failure, target, test flag, error message, and trigger time.
- `status_page_endpoints` stores the explicit endpoint allowlist for each public status page.

### Plan Summary
| Item | FREE | PRO |
|---|---:|---:|
| Max projects per workspace | 3 | 50 |
| Max endpoints per project | 5 | 50 |
| Min check interval (sec) | 300 | 60 |
| Max alert channels per endpoint | 1 | unlimited |
| Max workspace members | 1 | unlimited |
| Data retention (days) | 7 | 90 |
