# APIGuard Backend: 기본 회원 기능 구현 순서 체크리스트

이 체크리스트는 개발자가 직접 구현할 때 순서대로 따라가며 검증할 수 있도록 구성했습니다.

## 1. `/users/me` 정리
- [ ] `UserController`에서 `/users/me`의 이메일 파라미터 제거
- [ ] 인증된 사용자 정보만으로 조회하도록 `UserService.getUserDetail()` 연동
- [ ] 토큰 없이 호출 시 401 또는 403 확인

## 2. 인증 판별 로직 보강
- [ ] `authentication == null` 체크 추가
- [ ] 익명 인증 판단 (`AnonymousAuthenticationToken` 또는 `principal == "anonymousUser"`)
- [ ] 미인증 시 명확한 예외 발생

## 3. 비밀번호 변경 API 추가
- [ ] DTO 생성 (`currentPassword`, `newPassword`, `newPasswordConfirm`)
- [ ] 패스워드 규칙 검증 적용 (회원가입 규칙 재사용)
- [ ] 서비스 로직 구현 (현재 비밀번호 검증 -> 새 비밀번호 저장)
- [ ] 컨트롤러 엔드포인트 추가 (`PATCH /users/me/password`)
- [ ] 성공/실패 케이스 테스트

## 4. 예외 처리와 HTTP 상태 코드 정리
- [ ] `GlobalExceptionHandler`에서 상태 코드 반영
- [ ] `DuplicateEmailException` -> 409
- [ ] `InvalidCredentialsException` -> 401
- [ ] `UserNotFoundException` -> 404
- [ ] 미인증/권한 부족 -> 401/403

## 5. JWT vs Session 정책 결정
- [ ] 무상태 JWT로 갈지 결정
- [ ] 무상태일 경우 `SessionCreationPolicy.STATELESS` 적용
- [ ] 세션을 유지한다면 사용 이유/범위를 문서화

## 6. DB 스키마 정합성 확인
- [ ] 신규 DB에서 `users.id` 타입이 BIGINT인지 확인
- [ ] 초기 마이그레이션에서 `BIGSERIAL` 사용 여부 검토

## 7. whitelist 점검
- [ ] 로그인/회원가입/헬스체크가 whitelist에 포함되는지 확인
- [ ] Swagger 및 개발용 엔드포인트는 dev 프로필에서만 허용

## 8. 수동 테스트 시나리오
- [ ] 회원가입 -> 로그인 -> `/users/me` 조회
- [ ] 닉네임 변경 -> `/users/me` 재조회
- [ ] 비밀번호 변경 -> 이전/새 비밀번호로 로그인 테스트
- [ ] 토큰 없이 보호 API 접근 시 401/403 확인

---

원하면 이 체크리스트에 “API 요청 예시(curl)” 섹션도 추가해줄 수 있습니다.
