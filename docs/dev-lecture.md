# APIGuard Backend: Basic User Features Dev Lecture

This note summarizes what still needs development or cleanup in the current codebase, and how to implement it yourself.
It does not apply changes; it only explains what to do and where.

## 1) `/users/me` design (remove email parameter)

Problem:
- `getUser(@Valid @Email String email)` in the controller is not bound to any request field and can be ignored.
- A “me” endpoint should always identify the user by the JWT subject, not by a request parameter.

Goal:
- Only trust the authenticated principal and remove request parameters for identity.

Where:
- `src/main/java/com/apiguard/backend/domain/user/controller/UserController.java`
- `src/main/java/com/apiguard/backend/domain/user/service/UserService.java`

Implementation idea:
- Remove the `email` parameter from `/users/me`.
- In the service, extract the email from `SecurityContext` or use `@AuthenticationPrincipal` in the controller.

## 2) Improve authentication checks

Problem:
- `authentication.isAuthenticated()` can still be true for anonymous authentication.

Goal:
- Distinguish real authenticated users from anonymous ones.

Where:
- `src/main/java/com/apiguard/backend/domain/user/service/UserService.java`

Implementation idea:
- Check `authentication == null`.
- Check for `AnonymousAuthenticationToken` or `principal == "anonymousUser"`.

## 3) Add password change feature

Goal:
- Provide a standard “change password” endpoint for logged-in users.

Design:
- Endpoint: `PATCH /users/me/password` (recommended)
- Request: `currentPassword`, `newPassword`, `newPasswordConfirm`

Where:
- DTOs: `src/main/java/com/apiguard/backend/domain/user/dto`
- Service: `src/main/java/com/apiguard/backend/domain/user/service/UserService.java`
- Controller: `src/main/java/com/apiguard/backend/domain/user/controller/UserController.java`

Flow:
- Validate `currentPassword` matches stored password.
- Validate `newPassword` format (reuse signup rules).
- Validate `newPassword == newPasswordConfirm`.
- Update password using `PasswordEncoder`.

## 4) Proper HTTP status codes in error responses

Problem:
- Current `GlobalExceptionHandler` always returns 200 with error payload.

Goal:
- Align HTTP status codes with error type.

Suggested mapping:
- `DuplicateEmailException` -> 409
- `InvalidCredentialsException` -> 401
- `UserNotFoundException` -> 404
- Auth failure -> 401, permission failure -> 403

Where:
- `src/main/java/com/apiguard/backend/global/exception/GlobalExceptionHandler.java`

Implementation idea:
- Return `ResponseEntity<ApiResponse<...>>` with proper status codes
- Or annotate custom exceptions with `@ResponseStatus`

## 5) JWT vs Session (Redis) strategy

Current state:
- JWT auth filter is used
- `spring-session-data-redis` is enabled

Decision point:
- If you want **stateless JWT**, configure `SessionCreationPolicy.STATELESS` and avoid session storage.
- If you keep sessions, manage them deliberately (harder to reason about).

Where:
- `src/main/java/com/apiguard/backend/global/config/SecurityConfig.java`
- `build.gradle` (if you remove session storage)

## 6) DB schema alignment

Problem:
- `User.id` is `Long`, but original SQL used `SERIAL` (INT).

Current fix:
- A Flyway migration alters `id` to `BIGINT`.

If you reinitialize DB:
- Use `BIGSERIAL` in the initial migration to match the entity type.

Where:
- `src/main/resources/db/migration/V1__init.sql`

## 7) Whitelist management

Current security rule:
- Whitelisted endpoints are `permitAll`, everything else is authenticated.

Checklist:
- Login, signup, health should be whitelisted.
- Swagger and dev-only endpoints should be in `application-dev.yml` only.

Where:
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`

## 8) Suggested manual test flow

- Signup -> Login -> `/users/me` -> Update nickname -> `/users/me` again
- Change password -> Login with old/new password
- Try accessing protected endpoints without token

---

If you want, we can expand any section into an implementation guide with example code snippets and step-by-step steps.
