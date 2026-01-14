# 🎓 회원 기능 개선 강의 - Step by Step

> 각 챕터를 순서대로 진행하세요. 직접 코딩하면서 따라와 주세요!

---

## 📚 목차

| 순서 | 주제                                                                      | 파일                                      |
| :--: | ------------------------------------------------------------------------- | ----------------------------------------- |
|  1   | [비밀번호 유효성 검사 수정](#1-비밀번호-유효성-검사-수정)                 | `SignUpRequest.java`                      |
|  2   | [DuplicateEmailException 실제 적용](#2-duplicateemailexception-실제-적용) | `UserService.java`                        |
|  3   | [SignUp 응답값 수정](#3-signup-응답값-수정)                               | `UserService.java`, `UserController.java` |
|  4   | [로그인 API 구현](#4-로그인-api-구현)                                     | 신규 파일들                               |
|  5   | [SecurityConfig 인가 설정](#5-securityconfig-인가-설정)                   | `SecurityConfig.java`                     |
|  6   | [Refresh Token 저장/재발급](#6-refresh-token-저장재발급)                  | 추후 진행                                 |
|  7   | [테스트 케이스 보강](#7-테스트-케이스-보강)                               | 추후 진행                                 |

---

## 1. 비밀번호 유효성 검사 수정

### 📍 현재 문제점

```java
// SignUpRequest.java (현재)
@Size(message = "비밀번호는 영문, 숫자, 특수문자 조합으로 8~16자리여야 합니다.")
String password
```

`@Size`에 `min`, `max`가 없어서 **어떤 비밀번호도 통과**합니다!

### 🎯 해야 할 일

[SignUpRequest.java](file:///home/ksy/wsl-workspace/apiguard/apiguard-backend/src/main/java/com/apiguard/backend/domain/user/dto/SignUpRequest.java) 파일을 열고:

1. `@Size`에 `min = 8, max = 16` 추가
2. `@Pattern`을 추가해서 영문+숫자+특수문자 조합 강제

### ✏️ 수정 코드

```java
import jakarta.validation.constraints.Pattern;  // 추가!

public record SignUpRequest(
    @NotBlank(message = "이메일은 필수 입력사항입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    String email,

    @NotBlank(message = "비밀번호는 필수 입력사항입니다.")
    @Size(min = 8, max = 16, message = "비밀번호는 8~16자리여야 합니다.")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&]).+$",
        message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다."
    )
    String password,

    @NotBlank(message = "닉네임은 필수 입력사항입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요.")
    String nickname
) {}
```

### 💡 개념 설명: Bean Validation

| 어노테이션  | 용도                                |
| ----------- | ----------------------------------- |
| `@NotBlank` | 공백 불가 (null, "", " " 모두 차단) |
| `@Size`     | 문자열 길이 제한                    |
| `@Pattern`  | 정규표현식으로 형식 검증            |
| `@Email`    | 이메일 형식 검증                    |

### ✅ 완료 후 테스트

```http
POST /users/signup
Content-Type: application/json

{
  "email": "test@email.com",
  "password": "1234",
  "nickname": "테스트"
}
```

위 요청 시 **400 Bad Request**가 나와야 성공!

---

## 2. DuplicateEmailException 실제 적용

### 📍 현재 문제점

```java
// UserService.java (현재)
if (userRepository.existsByEmail(signUpRequest.email())) {
    throw new RuntimeException("이미 사용 중인 이메일입니다.");  // ← 일반 RuntimeException
}
```

`DuplicateEmailException`을 만들어놓고 사용하지 않고 있어요!

### 🎯 해야 할 일

[UserService.java](file:///home/ksy/wsl-workspace/apiguard/apiguard-backend/src/main/java/com/apiguard/backend/domain/user/service/UserService.java) 파일의 `signUp` 메서드에서:

```java
// 수정 전
throw new RuntimeException("이미 사용 중인 이메일입니다.");

// 수정 후
throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
```

> 💡 **import 추가도 잊지 마세요!**
>
> ```java
> import com.apiguard.backend.global.exception.DuplicateEmailException;
> ```

### 💡 개념 설명: 커스텀 예외

커스텀 예외를 사용하면:

- `GlobalExceptionHandler`에서 **예외별로 다른 처리** 가능
- 로그에서 **에러 종류를 명확히 구분** 가능
- 클라이언트에 **적절한 HTTP 상태 코드** 반환 가능

### 📌 추가 개선 (선택)

`GlobalExceptionHandler`에서 HTTP 상태 코드도 설정하면 더 좋아요:

```java
@ExceptionHandler(DuplicateEmailException.class)
@ResponseStatus(HttpStatus.CONFLICT)  // 409 Conflict
public ApiResponse<Void> handleDuplicateEmailException(DuplicateEmailException e) {
    log.warn("중복 이메일 에러");
    return ApiResponse.error(e.getMessage());
}
```

---

## 3. SignUp 응답값 수정

### 📍 현재 문제점

```java
// UserController.java (현재)
@PostMapping("/signup")
public ApiResponse<Long> signUp(@RequestBody @Valid SignUpRequest request) {
    userService.signUp(request);
    return ApiResponse.ok(1L);  // ← 항상 1L??
}
```

### 🎯 해야 할 일

**Step 1**: `UserService.signUp()`이 생성된 User ID를 반환하도록 수정

```java
// UserService.java
@Transactional
public Long signUp(SignUpRequest signUpRequest) {  // void → Long

    if (userRepository.existsByEmail(signUpRequest.email())) {
        throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
    }

    String encodedPassword = passwordEncoder.encode(signUpRequest.password());

    User user = User.builder()
        .email(signUpRequest.email())
        .nickname(signUpRequest.nickname())
        .password(encodedPassword)
        .role(Role.USER)
        .build();

    User savedUser = userRepository.save(user);  // 저장된 User 받기
    return savedUser.getId();  // ID 반환
}
```

**Step 2**: `UserController`에서 반환값 사용

```java
// UserController.java
@PostMapping("/signup")
public ApiResponse<Long> signUp(@RequestBody @Valid SignUpRequest request) {
    Long userId = userService.signUp(request);  // 반환값 받기
    return ApiResponse.ok(userId);  // 실제 ID 반환
}
```

---

## 4. 로그인 API 구현

### 📍 현재 문제점

회원가입은 있지만 **로그인 기능이 없습니다!**

### 🎯 해야 할 일 (4개 파일)

#### 4-1. LoginRequest.java (신규 생성)

📁 위치: `src/main/java/com/apiguard/backend/domain/user/dto/LoginRequest.java`

```java
package com.apiguard.backend.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "이메일은 필수 입력사항입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    String email,

    @NotBlank(message = "비밀번호는 필수 입력사항입니다.")
    String password
) {}
```

#### 4-2. LoginResponse.java (신규 생성)

📁 위치: `src/main/java/com/apiguard/backend/domain/user/dto/LoginResponse.java`

```java
package com.apiguard.backend.domain.user.dto;

public record LoginResponse(
    String accessToken,
    String refreshToken
) {}
```

#### 4-3. UserService.java에 login 메서드 추가

```java
// UserService.java에 추가

public LoginResponse login(LoginRequest request) {
    // 1. 이메일로 사용자 조회
    User user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> new RuntimeException("이메일 또는 비밀번호가 일치하지 않습니다."));

    // 2. 비밀번호 검증
    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
        throw new RuntimeException("이메일 또는 비밀번호가 일치하지 않습니다.");
    }

    // 3. JWT 토큰 생성
    String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name());
    String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

    return new LoginResponse(accessToken, refreshToken);
}
```

> ⚠️ **필요한 것들:**
>
> - `JwtTokenProvider` 주입 필요
> - `LoginRequest`, `LoginResponse` import 필요

#### 4-4. UserController.java에 login 엔드포인트 추가

```java
// UserController.java에 추가

@PostMapping("/login")
public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
    LoginResponse response = userService.login(request);
    return ApiResponse.ok(response);
}
```

### ✅ 완료 후 테스트

```http
### 1. 회원가입
POST /users/signup
Content-Type: application/json

{
  "email": "test@email.com",
  "password": "Test1234!",
  "nickname": "테스트"
}

### 2. 로그인
POST /users/login
Content-Type: application/json

{
  "email": "test@email.com",
  "password": "Test1234!"
}
```

응답 예시:

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

---

## 5. SecurityConfig 인가 설정

### 📍 현재 문제점

```java
// SecurityConfig.java (현재)
.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
```

**모든 요청을 허용**하고 있어서 JWT 인증이 의미가 없어요!

### 🎯 해야 할 일

[SecurityConfig.java](file:///home/ksy/wsl-workspace/apiguard/apiguard-backend/src/main/java/com/apiguard/backend/global/config/SecurityConfig.java)에서 인가 규칙 수정:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        // 인가 규칙 설정
        .authorizeHttpRequests(auth -> auth
            // 인증 없이 접근 가능한 경로
            .requestMatchers("/users/signup", "/users/login").permitAll()
            .requestMatchers("/auth/**").permitAll()
            // 나머지는 인증 필요
            .anyRequest().authenticated()
        )
        .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                         UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

### 💡 개념 설명: 인증 vs 인가

| 개념     | 영어           | 설명                         |
| -------- | -------------- | ---------------------------- |
| **인증** | Authentication | 너 누구야? (로그인)          |
| **인가** | Authorization  | 너 이거 해도 돼? (권한 검사) |

---

## 6. Refresh Token 저장/재발급

> 📘 **이 부분은 고급 주제입니다. 4번까지 완료 후 진행하세요.**

아직 구현이 필요한 부분:

- Redis 또는 DB에 Refresh Token 저장
- `POST /auth/refresh` 엔드포인트 구현
- Refresh Token 검증 및 Access Token 재발급
- 로그아웃 시 Refresh Token 삭제

---

## 7. 테스트 케이스 보강

> 📘 **기능 구현 후 진행하세요.**

추가 필요한 테스트:

- 중복 이메일 회원가입 실패 케이스
- 로그인 성공/실패 테스트
- JWT 토큰 검증 테스트
- Controller 통합 테스트

---

## 🎯 진행 체크리스트

아래 순서대로 진행하세요:

- [ ] 1. `SignUpRequest.java` - `@Size`, `@Pattern` 수정
- [ ] 2. `UserService.java` - `DuplicateEmailException` 사용
- [ ] 3. `UserService.java` + `UserController.java` - 응답값 수정
- [ ] 4-1. `LoginRequest.java` 생성
- [ ] 4-2. `LoginResponse.java` 생성
- [ ] 4-3. `UserService.login()` 메서드 추가
- [ ] 4-4. `UserController.login()` 엔드포인트 추가
- [ ] 5. `SecurityConfig.java` - 인가 규칙 수정

---

**질문이 있으면 언제든 물어보세요!** 🚀
