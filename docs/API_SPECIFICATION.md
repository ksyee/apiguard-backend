# APIGuard Backend API 명세서

**Base URL**: `http://localhost:8080` (개발) / `https://api.apiguard.com` (운영)
**인증 방식**: Bearer Token (JWT)
**공통 응답 형식**: `ApiResponse<T>`

---

## 공통 사항

### 응답 래퍼

모든 API 응답은 `ApiResponse<T>` 형식으로 래핑됩니다.

```json
// 성공 (데이터 포함)
{
  "success": true,
  "data": { ... }
}

// 성공 (데이터 없음)
{
  "success": true
}

// 실패
{
  "success": false,
  "message": "에러 메시지"
}
```

### 에러 응답 (ProblemDetail)

Validation 실패 및 비즈니스 예외 시 RFC 7807 형식으로 응답합니다.

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "엔드포인트를 찾을 수 없습니다.",
  "instance": "/endpoints/999",
  "code": "ENDPOINT_NOT_FOUND"
}
```

#### 에러 코드 목록

| code                    | HTTP Status | 설명                             |
| ----------------------- | ----------- | -------------------------------- |
| `VALIDATION_ERROR`      | 400         | 요청 데이터 검증 실패            |
| `UNAUTHORIZED`          | 401         | 인증 필요                        |
| `INVALID_CREDENTIALS`   | 401         | 로그인 실패                      |
| `FORBIDDEN`             | 403         | 권한 없음 (다른 사용자의 리소스) |
| `USER_NOT_FOUND`        | 404         | 사용자 없음                      |
| `PROJECT_NOT_FOUND`     | 404         | 프로젝트 없음                    |
| `ENDPOINT_NOT_FOUND`    | 404         | 엔드포인트 없음                  |
| `ALERT_NOT_FOUND`       | 404         | 알림 설정 없음                   |
| `DUPLICATE_EMAIL`       | 409         | 이메일 중복                      |
| `INTERNAL_SERVER_ERROR` | 500         | 서버 내부 오류                   |

### 인증

- 로그인 시 `accessToken`과 `refreshToken`을 발급받습니다.
- 인증이 필요한 API 호출 시 `Authorization: Bearer {accessToken}` 헤더를 포함합니다.
- accessToken 만료 시 `/auth/refresh`로 갱신합니다.

#### 인증 불필요 경로

- `POST /auth/login`
- `POST /users/signup`
- `POST /auth/refresh`
- `GET /health`
- `GET /swagger-ui/**` (개발 환경)
- `GET /v3/api-docs/**` (개발 환경)

### Enum 값

#### HttpMethod

`GET` | `POST` | `PUT` | `PATCH` | `DELETE` | `HEAD` | `OPTIONS`

#### CheckStatus

`SUCCESS` | `FAILURE` | `TIMEOUT` | `ERROR`

#### AlertType

`EMAIL` | `SLACK`

### 날짜/시간 형식

모든 날짜/시간 필드는 ISO 8601 형식입니다: `2025-01-15T14:30:00`

---

## 1. 인증 API

### 1.1 회원가입

**`POST /users/signup`** (인증 불필요)

회원가입 후 사용자 ID를 반환합니다.

**Request Body**

```json
{
  "email": "user@example.com",
  "password": "Password1!",
  "nickname": "홍길동"
}
```

| 필드     | 타입   | 필수 | 검증 규칙                                |
| -------- | ------ | ---- | ---------------------------------------- |
| email    | string | O    | 이메일 형식                              |
| password | string | O    | 8-20자, 대문자+소문자+숫자+특수문자 포함 |
| nickname | string | O    | 2-20자                                   |

**Response** `200 OK`

```json
{
  "success": true,
  "data": 1
}
```

---

### 1.2 로그인

**`POST /auth/login`** (인증 불필요)

**Request Body**

```json
{
  "email": "user@example.com",
  "password": "Password1!"
}
```

| 필드     | 타입   | 필수 | 검증 규칙   |
| -------- | ------ | ---- | ----------- |
| email    | string | O    | 이메일 형식 |
| password | string | O    | 필수        |

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "accessToken": "**REMOVED-FROM-HISTORY**",
    "refreshToken": "**REMOVED-FROM-HISTORY**"
  }
}
```

---

### 1.3 토큰 갱신

**`POST /auth/refresh`** (인증 불필요)

**Request Body**

```json
{
  "refreshToken": "**REMOVED-FROM-HISTORY**"
}
```

| 필드         | 타입   | 필수 |
| ------------ | ------ | ---- |
| refreshToken | string | O    |

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "accessToken": "**REMOVED-FROM-HISTORY**",
    "refreshToken": "**REMOVED-FROM-HISTORY**"
  }
}
```

---

### 1.4 로그아웃

**`POST /auth/logout`** (인증 필요)

**Request Body**

```json
{
  "refreshToken": "**REMOVED-FROM-HISTORY**"
}
```

| 필드         | 타입   | 필수 |
| ------------ | ------ | ---- |
| refreshToken | string | O    |

**Response** `200 OK`

```json
{
  "success": true
}
```

---

## 2. 사용자 API

### 2.1 내 정보 조회

**`GET /users/me`** (인증 필요)

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "nickname": "홍길동",
    "createdAt": "2025-01-15T14:30:00"
  }
}
```

---

### 2.2 내 정보 수정

**`PATCH /users/me`** (인증 필요)

**Request Body**

```json
{
  "nickname": "새닉네임"
}
```

| 필드     | 타입   | 필수 | 검증 규칙 |
| -------- | ------ | ---- | --------- |
| nickname | string | X    | 2-20자    |

**Response** `200 OK`

```json
{
  "success": true
}
```

---

### 2.3 비밀번호 변경

**`PATCH /users/me/password`** (인증 필요)

**Request Body**

```json
{
  "currentPassword": "OldPassword1!",
  "newPassword": "NewPassword1!",
  "newPasswordConfirm": "NewPassword1!"
}
```

| 필드               | 타입   | 필수 | 검증 규칙                                |
| ------------------ | ------ | ---- | ---------------------------------------- |
| currentPassword    | string | O    | 필수                                     |
| newPassword        | string | O    | 8-20자, 대문자+소문자+숫자+특수문자 포함 |
| newPasswordConfirm | string | O    | 필수                                     |

**Response** `200 OK`

```json
{
  "success": true
}
```

---

### 2.4 회원 탈퇴

**`DELETE /users/me`** (인증 필요)

**Response** `200 OK`

```json
{
  "success": true
}
```

---

## 3. 프로젝트 API

### 3.1 프로젝트 생성

**`POST /projects`** (인증 필요)

**Request Body**

```json
{
  "name": "내 프로젝트",
  "description": "프로젝트 설명입니다."
}
```

| 필드        | 타입   | 필수 | 검증 규칙  |
| ----------- | ------ | ---- | ---------- |
| name        | string | O    | 최대 100자 |
| description | string | X    | 최대 500자 |

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "내 프로젝트",
    "description": "프로젝트 설명입니다.",
    "createdAt": "2025-01-15T14:30:00"
  }
}
```

---

### 3.2 내 프로젝트 목록 조회

**`GET /projects`** (인증 필요)

**Response** `200 OK`

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "내 프로젝트",
      "description": "프로젝트 설명입니다.",
      "createdAt": "2025-01-15T14:30:00"
    }
  ]
}
```

---

### 3.3 프로젝트 상세 조회

**`GET /projects/{id}`** (인증 필요)

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | Long | 프로젝트 ID |

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "내 프로젝트",
    "description": "프로젝트 설명입니다.",
    "createdAt": "2025-01-15T14:30:00"
  }
}
```

---

### 3.4 프로젝트 수정

**`PUT /projects/{id}`** (인증 필요)

**Request Body**

```json
{
  "name": "수정된 이름",
  "description": "수정된 설명"
}
```

| 필드        | 타입   | 필수 | 검증 규칙  |
| ----------- | ------ | ---- | ---------- |
| name        | string | X    | 최대 100자 |
| description | string | X    | 최대 500자 |

**Response** `200 OK` — ProjectResponse 동일

---

### 3.5 프로젝트 삭제

**`DELETE /projects/{id}`** (인증 필요, soft delete)

**Response** `200 OK`

```json
{
  "success": true
}
```

---

## 4. 엔드포인트 API

### 4.1 엔드포인트 생성

**`POST /projects/{projectId}/endpoints`** (인증 필요)

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| projectId | Long | 프로젝트 ID |

**Request Body**

```json
{
  "url": "https://api.example.com/health",
  "httpMethod": "GET",
  "headers": "{\"Authorization\": \"Bearer token\"}",
  "body": null,
  "expectedStatusCode": 200,
  "checkInterval": 60
}
```

| 필드               | 타입    | 필수 | 검증 규칙        | 기본값 |
| ------------------ | ------- | ---- | ---------------- | ------ |
| url                | string  | O    | URL 형식         | -      |
| httpMethod         | string  | O    | HttpMethod enum  | -      |
| headers            | string  | X    | JSON 문자열      | null   |
| body               | string  | X    | -                | null   |
| expectedStatusCode | integer | X    | 100-599          | 200    |
| checkInterval      | integer | X    | 1 이상 (초 단위) | 60     |

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "id": 1,
    "projectId": 1,
    "url": "https://api.example.com/health",
    "httpMethod": "GET",
    "headers": "{\"Authorization\": \"Bearer token\"}",
    "body": null,
    "expectedStatusCode": 200,
    "checkInterval": 60,
    "isActive": true,
    "lastCheckedAt": null,
    "createdAt": "2025-01-15T14:30:00"
  }
}
```

---

### 4.2 프로젝트 내 엔드포인트 목록 조회

**`GET /projects/{projectId}/endpoints`** (인증 필요)

**Response** `200 OK`

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "projectId": 1,
      "url": "https://api.example.com/health",
      "httpMethod": "GET",
      "headers": null,
      "body": null,
      "expectedStatusCode": 200,
      "checkInterval": 60,
      "isActive": true,
      "lastCheckedAt": "2025-01-15T15:00:00",
      "createdAt": "2025-01-15T14:30:00"
    }
  ]
}
```

---

### 4.3 엔드포인트 상세 조회

**`GET /endpoints/{id}`** (인증 필요)

**Response** `200 OK` — EndpointResponse 동일

---

### 4.4 엔드포인트 수정

**`PUT /endpoints/{id}`** (인증 필요)

**Request Body** — CreateEndpointRequest와 동일 구조, 모든 필드 선택

**Response** `200 OK` — EndpointResponse 동일

---

### 4.5 엔드포인트 삭제

**`DELETE /endpoints/{id}`** (인증 필요, soft delete)

**Response** `200 OK`

```json
{
  "success": true
}
```

---

### 4.6 엔드포인트 활성화/비활성화 토글

**`PATCH /endpoints/{id}/toggle`** (인증 필요)

**Request Body**: 없음

**Response** `200 OK` — EndpointResponse (isActive 값이 반전됨)

---

## 5. 헬스체크 API

### 5.1 즉시 체크 실행

**`POST /endpoints/{id}/test`** (인증 필요)

엔드포인트를 즉시 체크하고 결과를 반환합니다. 결과는 DB에도 저장됩니다.

**Request Body**: 없음

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "id": 42,
    "endpointId": 1,
    "status": "SUCCESS",
    "statusCode": 200,
    "responseTimeMs": 156,
    "errorMessage": null,
    "checkedAt": "2025-01-15T15:00:00"
  }
}
```

---

### 5.2 최근 체크 결과 목록

**`GET /endpoints/{id}/checks?limit=20`** (인증 필요)

**Query Parameter**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| limit | int | X | 20 | 조회할 개수 |

**Response** `200 OK`

```json
{
  "success": true,
  "data": [
    {
      "id": 42,
      "endpointId": 1,
      "status": "SUCCESS",
      "statusCode": 200,
      "responseTimeMs": 156,
      "errorMessage": null,
      "checkedAt": "2025-01-15T15:00:00"
    },
    {
      "id": 41,
      "endpointId": 1,
      "status": "FAILURE",
      "statusCode": 500,
      "responseTimeMs": 2034,
      "errorMessage": null,
      "checkedAt": "2025-01-15T14:59:00"
    }
  ]
}
```

---

### 5.3 엔드포인트 통계 (24시간)

**`GET /endpoints/{id}/stats`** (인증 필요)

최근 24시간 기준 통계를 반환합니다.

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "totalChecks": 1440,
    "successCount": 1435,
    "successRate": 99.65,
    "avgResponseTimeMs": 142.5,
    "since": "2025-01-14T15:00:00"
  }
}
```

---

### 5.4 시간별 통계

**`GET /endpoints/{id}/stats/hourly`** (인증 필요)

최근 24시간을 시간 단위로 집계한 통계입니다.

**Response** `200 OK`

```json
{
  "success": true,
  "data": [
    {
      "hour": "2025-01-15T14:00:00",
      "checkCount": 60,
      "successCount": 59,
      "avgResponseTimeMs": 145.2
    },
    {
      "hour": "2025-01-15T13:00:00",
      "checkCount": 60,
      "successCount": 60,
      "avgResponseTimeMs": 132.8
    }
  ]
}
```

---

### 5.5 프로젝트 통계

**`GET /projects/{id}/stats`** (인증 필요)

프로젝트 내 모든 엔드포인트의 현재 상태를 합산한 통계입니다.

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "totalEndpoints": 5,
    "upCount": 4,
    "downCount": 1,
    "avgResponseTimeMs": 165.3
  }
}
```

---

## 6. 알림 설정 API

### 6.1 알림 설정 생성

**`POST /endpoints/{endpointId}/alerts`** (인증 필요)

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| endpointId | Long | 엔드포인트 ID |

**Request Body**

```json
{
  "alertType": "EMAIL",
  "target": "alert@example.com",
  "threshold": 3
}
```

| 필드      | 타입    | 필수 | 검증 규칙            | 기본값 | 설명                               |
| --------- | ------- | ---- | -------------------- | ------ | ---------------------------------- |
| alertType | string  | O    | `EMAIL` 또는 `SLACK` | -      | 알림 유형                          |
| target    | string  | O    | 필수                 | -      | 이메일 주소 또는 Slack Webhook URL |
| threshold | integer | X    | 1 이상               | 3      | 연속 실패 횟수 임계값              |

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "id": 1,
    "endpointId": 1,
    "alertType": "EMAIL",
    "target": "alert@example.com",
    "threshold": 3,
    "isActive": true,
    "createdAt": "2025-01-15T14:30:00"
  }
}
```

---

### 6.2 엔드포인트 알림 설정 목록 조회

**`GET /endpoints/{endpointId}/alerts`** (인증 필요)

**Response** `200 OK`

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "endpointId": 1,
      "alertType": "EMAIL",
      "target": "alert@example.com",
      "threshold": 3,
      "isActive": true,
      "createdAt": "2025-01-15T14:30:00"
    },
    {
      "id": 2,
      "endpointId": 1,
      "alertType": "SLACK",
      "target": "https://hooks.slack.com/services/T.../B.../xxx",
      "threshold": 5,
      "isActive": true,
      "createdAt": "2025-01-15T14:35:00"
    }
  ]
}
```

---

### 6.3 알림 설정 수정

**`PUT /alerts/{id}`** (인증 필요)

**Request Body**

```json
{
  "alertType": "SLACK",
  "target": "https://hooks.slack.com/services/T.../B.../xxx",
  "threshold": 5
}
```

| 필드      | 타입    | 필수 | 검증 규칙            |
| --------- | ------- | ---- | -------------------- |
| alertType | string  | X    | `EMAIL` 또는 `SLACK` |
| target    | string  | X    | -                    |
| threshold | integer | X    | 1 이상               |

**Response** `200 OK` — AlertResponse 동일

---

### 6.4 알림 설정 삭제

**`DELETE /alerts/{id}`** (인증 필요, soft delete)

**Response** `200 OK`

```json
{
  "success": true
}
```

---

### 6.5 알림 설정 활성화/비활성화 토글

**`PATCH /alerts/{id}/toggle`** (인증 필요)

**Request Body**: 없음

**Response** `200 OK` — AlertResponse (isActive 값이 반전됨)

---

## 7. 워크스페이스 및 멤버 관리 API

### 공통 사항

#### WorkspaceResponse

```json
{
  "id": 1,
  "name": "My Workspace",
  "slug": "my-workspace",
  "createdAt": "2025-01-15T14:30:00"
}
```

| 필드      | 타입   | 설명                                              |
| --------- | ------ | ------------------------------------------------- |
| id        | Long   | 워크스페이스 ID                                   |
| name      | string | 워크스페이스 이름                                 |
| slug      | string | URL용 식별자 (name 기반 자동 생성, 영문/숫자/하이픈) |
| createdAt | string | 생성일시 (ISO 8601)                               |

#### WorkspaceMemberResponse

```json
{
  "id": 1,
  "userId": 1,
  "email": "user@example.com",
  "nickname": "홍길동",
  "role": "owner",
  "joinedAt": "2025-01-15T14:30:00"
}
```

| 필드     | 타입   | 설명                                          |
| -------- | ------ | --------------------------------------------- |
| id       | Long   | 멤버십 ID (memberId로 사용)                   |
| userId   | Long   | 사용자 ID                                     |
| email    | string | 사용자 이메일                                 |
| nickname | string | 사용자 닉네임                                 |
| role     | string | 역할: `owner` \| `admin` \| `member` \| `viewer` |
| joinedAt | string | 워크스페이스 참여일시 (ISO 8601)              |

#### 역할 계층 및 권한

| 역할     | 워크스페이스 조회 | 멤버 목록 조회 | 멤버 초대/수정/제거 | 워크스페이스 삭제 |
| -------- | :---: | :---: | :---: | :---: |
| `owner`  | ✅ | ✅ | ✅ | ✅ |
| `admin`  | ✅ | ✅ | ✅ | ❌ |
| `member` | ✅ | ✅ | ❌ | ❌ |
| `viewer` | ✅ | ✅ | ❌ | ❌ |

> - `owner`는 초대로 부여할 수 없으며, 초대/역할변경 시 `admin` \| `member` \| `viewer`만 지정 가능
> - `owner`의 역할은 변경하거나 제거할 수 없음
> - 워크스페이스를 생성한 사용자는 자동으로 `owner`로 등록됨

---

### 7.1 워크스페이스 생성

**`POST /workspaces`** (인증 필요)

워크스페이스를 생성하고, 생성자를 `owner`로 자동 등록합니다.

**Request Body**

```json
{
  "name": "My Workspace"
}
```

| 필드 | 타입   | 필수 | 검증 규칙          |
| ---- | ------ | ---- | ------------------ |
| name | string | O    | 공백 불가, 최대 100자 |

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "My Workspace",
    "slug": "my-workspace",
    "createdAt": "2025-01-15T14:30:00"
  }
}
```

> `slug`는 `name`에서 자동 생성됩니다 (소문자 변환, 공백·특수문자 → `-`).
> 동일한 slug가 이미 존재하면 `-1`, `-2` 형태로 suffix가 붙습니다.

**Error Responses**

| 상태코드 | 설명 |
| -------- | ---- |
| 400 | 이름 미입력 또는 100자 초과 |
| 401 | 인증 필요 |

---

### 7.2 내 워크스페이스 목록 조회

**`GET /workspaces`** (인증 필요)

현재 사용자가 속한 모든 워크스페이스를 반환합니다 (역할 무관).

**Response** `200 OK`

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "My Workspace",
      "slug": "my-workspace",
      "createdAt": "2025-01-15T14:30:00"
    },
    {
      "id": 2,
      "name": "Team Workspace",
      "slug": "team-workspace",
      "createdAt": "2025-01-16T09:00:00"
    }
  ]
}
```

**Error Responses**

| 상태코드 | 설명 |
| -------- | ---- |
| 401 | 인증 필요 |

---

### 7.3 워크스페이스 상세 조회

**`GET /workspaces/{id}`** (인증 필요)

해당 워크스페이스의 멤버인 경우에만 조회 가능합니다.

**Path Parameter**

| 파라미터 | 타입 | 설명            |
| -------- | ---- | --------------- |
| id       | Long | 워크스페이스 ID |

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "My Workspace",
    "slug": "my-workspace",
    "createdAt": "2025-01-15T14:30:00"
  }
}
```

**Error Responses**

| 상태코드 | 설명 |
| -------- | ---- |
| 401 | 인증 필요 |
| 403 | 해당 워크스페이스의 멤버가 아님 |
| 404 | 워크스페이스 없음 |

---

### 7.4 워크스페이스 삭제

**`DELETE /workspaces/{id}`** (인증 필요, `owner`만 가능)

**Path Parameter**

| 파라미터 | 타입 | 설명            |
| -------- | ---- | --------------- |
| id       | Long | 워크스페이스 ID |

**Request Body**: 없음

**Response** `200 OK`

```json
{
  "success": true
}
```

**Error Responses**

| 상태코드 | 설명 |
| -------- | ---- |
| 401 | 인증 필요 |
| 403 | `owner`가 아님 또는 멤버가 아님 |
| 404 | 워크스페이스 없음 |

---

### 7.5 워크스페이스 멤버 목록 조회

**`GET /workspaces/{workspaceId}/members`** (인증 필요)

해당 워크스페이스의 멤버인 경우에만 조회 가능합니다.

**Path Parameter**

| 파라미터    | 타입 | 설명            |
| ----------- | ---- | --------------- |
| workspaceId | Long | 워크스페이스 ID |

**Response** `200 OK`

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": 1,
      "email": "owner@example.com",
      "nickname": "Owner User",
      "role": "owner",
      "joinedAt": "2025-01-15T14:30:00"
    },
    {
      "id": 2,
      "userId": 2,
      "email": "admin@example.com",
      "nickname": "Admin User",
      "role": "admin",
      "joinedAt": "2025-01-16T09:00:00"
    }
  ]
}
```

**Error Responses**

| 상태코드 | 설명 |
| -------- | ---- |
| 401 | 인증 필요 |
| 403 | 해당 워크스페이스의 멤버가 아님 |
| 404 | 워크스페이스 없음 |

---

### 7.6 멤버 초대

**`POST /workspaces/{workspaceId}/members/invite`** (인증 필요, `admin` 이상 권한 필요)

이미 가입된 사용자를 이메일로 워크스페이스에 초대합니다.

**Path Parameter**

| 파라미터    | 타입 | 설명            |
| ----------- | ---- | --------------- |
| workspaceId | Long | 워크스페이스 ID |

**Request Body**

```json
{
  "email": "new.member@example.com",
  "role": "member"
}
```

| 필드  | 타입   | 필수 | 검증 규칙                              |
| ----- | ------ | ---- | -------------------------------------- |
| email | string | O    | 이메일 형식, 서비스에 가입된 계정이어야 함 |
| role  | string | O    | `admin` \| `member` \| `viewer`        |

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "id": 3,
    "userId": 5,
    "email": "new.member@example.com",
    "nickname": "신규 멤버",
    "role": "member",
    "joinedAt": "2025-01-17T10:00:00"
  }
}
```

**Error Responses**

| 상태코드 | 설명 |
| -------- | ---- |
| 400 | 필드 검증 실패 또는 이미 워크스페이스 멤버인 사용자 |
| 401 | 인증 필요 |
| 403 | `admin` 이상 권한 없음 |
| 404 | 워크스페이스 없음 또는 해당 이메일의 사용자 없음 |

---

### 7.7 멤버 역할 변경

**`PUT /workspaces/{workspaceId}/members/{memberId}/role`** (인증 필요, `admin` 이상 권한 필요)

`owner`의 역할은 변경할 수 없습니다.

**Path Parameter**

| 파라미터    | 타입 | 설명                              |
| ----------- | ---- | --------------------------------- |
| workspaceId | Long | 워크스페이스 ID                   |
| memberId    | Long | 멤버십 ID (`WorkspaceMemberResponse.id`) |

**Request Body**

```json
{
  "role": "admin"
}
```

| 필드 | 타입   | 필수 | 검증 규칙                       |
| ---- | ------ | ---- | ------------------------------- |
| role | string | O    | `admin` \| `member` \| `viewer` |

**Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "id": 2,
    "userId": 3,
    "email": "user@example.com",
    "nickname": "홍길동",
    "role": "admin",
    "joinedAt": "2025-01-16T09:00:00"
  }
}
```

**Error Responses**

| 상태코드 | 설명 |
| -------- | ---- |
| 400 | 필드 검증 실패 |
| 401 | 인증 필요 |
| 403 | `admin` 이상 권한 없음 또는 `owner` 역할 변경 시도 |
| 404 | 워크스페이스 없음 또는 해당 멤버 없음 |

---

### 7.8 멤버 제거

**`DELETE /workspaces/{workspaceId}/members/{memberId}`** (인증 필요, `admin` 이상 권한 필요)

`owner`는 제거할 수 없습니다.

**Path Parameter**

| 파라미터    | 타입 | 설명                              |
| ----------- | ---- | --------------------------------- |
| workspaceId | Long | 워크스페이스 ID                   |
| memberId    | Long | 멤버십 ID (`WorkspaceMemberResponse.id`) |

**Request Body**: 없음

**Response** `200 OK`

```json
{
  "success": true
}
```

**Error Responses**

| 상태코드 | 설명 |
| -------- | ---- |
| 401 | 인증 필요 |
| 403 | `admin` 이상 권한 없음 또는 `owner` 제거 시도 |
| 404 | 워크스페이스 없음 또는 해당 멤버 없음 |

---

## API 엔드포인트 요약

### 인증 (Public)

| Method | URL             | 설명      |
| ------ | --------------- | --------- |
| POST   | `/users/signup` | 회원가입  |
| POST   | `/auth/login`   | 로그인    |
| POST   | `/auth/refresh` | 토큰 갱신 |

### 인증 (Protected)

| Method | URL            | 설명     |
| ------ | -------------- | -------- |
| POST   | `/auth/logout` | 로그아웃 |

### 사용자

| Method | URL                  | 설명          |
| ------ | -------------------- | ------------- |
| GET    | `/users/me`          | 내 정보 조회  |
| PATCH  | `/users/me`          | 내 정보 수정  |
| PATCH  | `/users/me/password` | 비밀번호 변경 |
| DELETE | `/users/me`          | 회원 탈퇴     |

### 워크스페이스 및 멤버 (추가됨)

| Method | URL                                                 | 설명              |
| ------ | --------------------------------------------------- | ----------------- |
| POST   | `/workspaces`                                       | 워크스페이스 생성 |
| GET    | `/workspaces`                                       | 워크스페이스 목록 |
| GET    | `/workspaces/{id}`                                  | 워크스페이스 상세 |
| DELETE | `/workspaces/{id}`                                  | 워크스페이스 삭제 |
| GET    | `/workspaces/{workspaceId}/members`                 | 멤버 목록 조회    |
| POST   | `/workspaces/{workspaceId}/members/invite`          | 멤버 초대         |
| PUT    | `/workspaces/{workspaceId}/members/{memberId}/role` | 멤버 역할 변경    |
| DELETE | `/workspaces/{workspaceId}/members/{memberId}`      | 멤버 제거         |

### 프로젝트

| Method | URL              | 설명             |
| ------ | ---------------- | ---------------- |
| POST   | `/projects`      | 프로젝트 생성    |
| GET    | `/projects`      | 내 프로젝트 목록 |
| GET    | `/projects/{id}` | 프로젝트 상세    |
| PUT    | `/projects/{id}` | 프로젝트 수정    |
| DELETE | `/projects/{id}` | 프로젝트 삭제    |

### 엔드포인트

| Method | URL                               | 설명            |
| ------ | --------------------------------- | --------------- |
| POST   | `/projects/{projectId}/endpoints` | 엔드포인트 생성 |
| GET    | `/projects/{projectId}/endpoints` | 엔드포인트 목록 |
| GET    | `/endpoints/{id}`                 | 엔드포인트 상세 |
| PUT    | `/endpoints/{id}`                 | 엔드포인트 수정 |
| DELETE | `/endpoints/{id}`                 | 엔드포인트 삭제 |
| PATCH  | `/endpoints/{id}/toggle`          | 활성화 토글     |

### 헬스체크

| Method | URL                               | 설명           |
| ------ | --------------------------------- | -------------- |
| POST   | `/endpoints/{id}/test`            | 즉시 체크 실행 |
| GET    | `/endpoints/{id}/checks?limit=20` | 최근 체크 결과 |
| GET    | `/endpoints/{id}/stats`           | 24시간 통계    |
| GET    | `/endpoints/{id}/stats/hourly`    | 시간별 통계    |
| GET    | `/projects/{id}/stats`            | 프로젝트 통계  |

### 알림 설정

| Method | URL                              | 설명             |
| ------ | -------------------------------- | ---------------- |
| POST   | `/endpoints/{endpointId}/alerts` | 알림 설정 생성   |
| GET    | `/endpoints/{endpointId}/alerts` | 알림 설정 목록   |
| PUT    | `/alerts/{id}`                   | 알림 설정 수정   |
| DELETE | `/alerts/{id}`                   | 알림 설정 삭제   |
| PATCH  | `/alerts/{id}/toggle`            | 알림 활성화 토글 |

---

**총 API 수**: 33개
**최종 업데이트**: 2026-02-25
