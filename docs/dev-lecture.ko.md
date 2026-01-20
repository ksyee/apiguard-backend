# APIGuard Backend: 기본 회원 기능 개발 강의

이 문서는 현재 코드베이스에서 추가 개발/정리가 필요한 부분을 정리하고, 직접 구현할 수 있도록 방향과 위치를 설명합니다.
코드를 바꾸지는 않으며 “어디를 어떻게 바꾸면 되는지”를 안내합니다.

## 1) `/users/me` 설계 (이메일 파라미터 제거)

문제:
- 컨트롤러의 `getUser(@Valid @Email String email)`은 요청 바인딩이 되지 않거나 무시될 수 있음.
- “내 정보”는 요청 파라미터가 아니라 JWT의 subject(이메일)로 결정해야 함.

목표:
- 인증된 사용자 정보만 신뢰하고, 파라미터로 ID/이메일을 받지 않기.

위치:
- `src/main/java/com/apiguard/backend/domain/user/controller/UserController.java`
- `src/main/java/com/apiguard/backend/domain/user/service/UserService.java`

구현 방향:
- `/users/me`에서 `email` 파라미터 제거
- 서비스에서 `SecurityContext`에서 이메일을 꺼내거나, 컨트롤러에서 `@AuthenticationPrincipal` 사용

## 2) 인증 상태 판별 개선

문제:
- `authentication.isAuthenticated()`는 익명 인증에서도 true일 수 있음.

목표:
- 진짜 로그인 사용자와 익명 사용자를 구분하기.

위치:
- `src/main/java/com/apiguard/backend/domain/user/service/UserService.java`

구현 방향:
- `authentication == null` 체크
- `AnonymousAuthenticationToken` 여부 또는 `principal == "anonymousUser"` 확인

## 3) 비밀번호 변경 기능 추가

목표:
- 로그인 사용자가 비밀번호를 변경할 수 있도록 기본 기능 제공

설계:
- 엔드포인트: `PATCH /users/me/password` (권장)
- 요청 필드: `currentPassword`, `newPassword`, `newPasswordConfirm`

위치:
- DTO: `src/main/java/com/apiguard/backend/domain/user/dto`
- 서비스: `src/main/java/com/apiguard/backend/domain/user/service/UserService.java`
- 컨트롤러: `src/main/java/com/apiguard/backend/domain/user/controller/UserController.java`

흐름:
- `currentPassword` 일치 확인
- `newPassword` 규칙 검사(회원가입 규칙 재사용)
- `newPassword == newPasswordConfirm` 검증
- `PasswordEncoder`로 비밀번호 업데이트

## 4) 에러 응답에 HTTP 상태 코드 반영

문제:
- `GlobalExceptionHandler`가 모든 예외를 200으로 반환할 가능성 큼.

목표:
- 예외 유형에 맞는 HTTP 상태 코드 반환

권장 매핑:
- `DuplicateEmailException` -> 409
- `InvalidCredentialsException` -> 401
- `UserNotFoundException` -> 404
- 인증 실패 -> 401, 권한 부족 -> 403

위치:
- `src/main/java/com/apiguard/backend/global/exception/GlobalExceptionHandler.java`

구현 방향:
- `ResponseEntity<ApiResponse<...>>` 반환으로 상태 코드 설정
- 또는 커스텀 예외에 `@ResponseStatus` 적용

## 5) JWT vs Session(Redis) 전략 정리

현재 상태:
- JWT 인증 필터 사용 중
- `spring-session-data-redis` 활성화

결정 포인트:
- **무상태 JWT**로 갈 경우: `SessionCreationPolicy.STATELESS` 설정 + 세션 저장소 제거/비활성화
- 세션과 혼합 사용할 경우: 정책과 흐름을 명확히 정의해야 함

위치:
- `src/main/java/com/apiguard/backend/global/config/SecurityConfig.java`
- `build.gradle` (세션 저장소 제거 시)

## 6) DB 스키마 정합성

문제:
- `User.id`는 `Long`인데 초기 SQL은 `SERIAL`(INT)

현재 해결:
- Flyway 마이그레이션으로 `BIGINT` 변경

신규 DB 기준:
- 초기 마이그레이션에서 `BIGSERIAL`을 쓰면 엔티티와 정합성 유지

위치:
- `src/main/resources/db/migration/V1__init.sql`

## 7) 화이트리스트 관리

현재 보안 규칙:
- whitelist만 `permitAll`, 나머지는 모두 인증 필요

체크리스트:
- 로그인, 회원가입, 헬스체크는 whitelist에 포함
- Swagger, 개발용 엔드포인트는 `application-dev.yml`에서만 허용

위치:
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`

## 8) 수동 테스트 흐름

- 회원가입 -> 로그인 -> `/users/me` -> 닉네임 변경 -> `/users/me` 재조회
- 비밀번호 변경 추가 시: 기존 비밀번호/새 비밀번호 로그인 테스트
- 토큰 없이 보호 API 접근 시 401/403 확인

---

필요하면 각 항목을 “구현 로드맵 + 예시 코드”까지 확장해서 설명해 드릴게요.
