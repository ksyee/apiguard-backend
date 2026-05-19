# Public Release Checklist

백엔드 저장소를 공개하거나 public mirror를 만들기 전에 아래 항목을 확인합니다.

## Current Repository Note

2026-05-19 기준 현재 파일에는 공개하면 안 되는 실제 운영 secret을 두지 않습니다. 다만 Git history에는 과거 로컬 개발용 DB 비밀번호, JWT fallback, Toss sandbox 형식 키가 남아 있습니다.

따라서 이 저장소를 그대로 public 전환하지 말고, 아래 중 하나를 선택합니다.

- history rewrite 후 public 전환
- 민감 이력이 없는 새 public mirror 생성
- private 유지 + 문서/API/핵심 코드 일부만 포트폴리오에 공개

## Configuration

- `src/main/resources/application-prod.yml`은 운영 secret을 환경변수로만 받습니다.
- `docker-compose.server.yml`은 `POSTGRES_PASSWORD`, `JWT_SECRET`, `MAIL_PASSWORD`, `TOSS_SECRET_KEY`, `TOSS_CLIENT_KEY`가 없으면 실행되지 않습니다.
- `src/main/resources/application-dev.yml`의 기본값은 로컬 개발용 dummy 값입니다.
- `src/test/resources/application.yml`의 key 값은 테스트 전용 dummy 값입니다.

## Secret Scan

현재 파일 기준으로 아래 패턴을 확인합니다.

```bash
rg -n "apiguard1234|test_sk_[A-Za-z0-9]|test_ck_[A-Za-z0-9]|05da0a|BEGIN .*PRIVATE KEY|AKIA|ghp_" . \
  --glob '!build/**' \
  --glob '!.gradle-local/**' \
  --glob '!docker/db/**' \
  --glob '!docs/public-release-checklist.md'
```

Git history까지 확인합니다.

```bash
git log -p --all -G "test_sk_|test_ck_|JWT_SECRET|PRIVATE KEY|AKIA|ghp_|apiguard1234"
```

민감 값이 history에 남아 있으면 public 전환보다 새 public mirror 생성 또는 history rewrite 후 공개를 선택합니다.

## External Exposure

- 운영 서버 주소, SSH 계정, private key는 GitHub Secrets/Variables에만 둡니다.
- `.env`는 커밋하지 않고 `.env.example`만 공개합니다.
- 데모 계정 비밀번호는 운영 계정과 재사용하지 않습니다.
- Toss, SMTP, DB credential은 실제 값을 커밋하지 않습니다.
