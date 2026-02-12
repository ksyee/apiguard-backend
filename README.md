# API Guard Backend

API 모니터링 및 보안을 위한 백엔드 서비스

## Tech Stack

| Category  | Technology        |
|-----------|-------------------|
| Language  | Java 21           |
| Framework | Spring Boot 4.0.1 |
| Database  | PostgreSQL 18     |
| Cache     | Redis 8           |
| Auth      | JWT (jjwt 0.12.6) |
| Migration | Flyway 11.1       |
| Build     | Gradle            |

## Project Structure

```
src/main/java/com/apiguard/backend
├── domain
│   ├── auth                    # 인증 도메인
│   │   ├── controller
│   │   ├── dto
│   │   └── service
│   └── user                    # 사용자 도메인
│       ├── controller
│       ├── dto
│       ├── entity
│       ├── repository
│       └── service
└── global
    ├── common                  # 공통 응답, 헬스체크
    ├── config                  # Security, JWT, Flyway 설정
    └── exception               # 전역 예외 처리
```

## Getting Started

### Prerequisites

- Java 21+
- Docker & Docker Compose

### Run

```bash
# 1. 인프라 실행 (PostgreSQL, Redis)
docker-compose up -d

# 2. 애플리케이션 실행
./gradlew bootRun
```

### Environment

필요한 환경 변수:

| Variable      | Description |
|---------------|-------------|
| `DB_URL`      | 데이터베이스 URL  |
| `DB_USERNAME` | 데이터베이스 사용자명 |
| `DB_PASSWORD` | 데이터베이스 비밀번호 |
| `JWT_SECRET`  | JWT 서명 키    |

## API Endpoints

### Auth

| Method | Endpoint      | Description |
|--------|---------------|-------------|
| POST   | `/auth/login` | 로그인         |

### User

| Method | Endpoint        | Description |
|--------|-----------------|-------------|
| POST   | `/users/signup` | 회원가입        |
| GET    | `/users/me`     | 내 정보 조회     |

## API Response Format

```json
{
  "data": {
    ...
  }
}
```

### Error Response

```json
{
  "message": "에러 메시지"
}
```

## Testing

```bash
./gradlew test
```

## Database Migration

Flyway를 사용한 마이그레이션. 파일 위치: `src/main/resources/db/migration/`

```
V1__init.sql              # 초기 스키마
V2__add_role_to_users.sql # Role 컬럼 추가
```

## License

MIT
