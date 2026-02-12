# APIGuard - API 모니터링 도구 개발 로드맵

## 📋 프로젝트 개요

**프로젝트명**: APIGuard  
**목표**: 3개월 내 MVP 완성 및 배포  
**일일 작업 시간**: 평일 3시간, 주말 6시간  
**총 예상 시간**: 220~280시간

---

## 🎯 핵심 목표

1. ✅ API 엔드포인트를 등록하고 주기적으로 헬스체크
2. ✅ 응답 시간, 상태 코드, 성공률 등 통계 제공
3. ✅ 장애 발생 시 이메일/Slack 알림
4. ✅ 실시간 대시보드로 모니터링 상태 시각화
5. ✅ 실제 배포 및 본인 프로젝트에 사용

---

## 🛠 기술 스택

### Frontend
- **Next.js 14** (App Router)
- **TypeScript**
- **Tailwind CSS**
- **shadcn/ui** (UI 컴포넌트)
- **Chart.js** / **Recharts** (차트)
- **React Hook Form** (폼 관리)
- **Zustand** or **Context API** (상태 관리)

### Backend
- **Spring Boot 3.2+**
- **Java 21**
- **Spring Security** (JWT)
- **Spring Data JPA**
- **Spring Scheduler**
- **RestTemplate** / **WebClient** (HTTP 요청)
- **Flyway** (DB 마이그레이션)

### Database
- **PostgreSQL** (Supabase 무료 티어)

### Cache
- **Redis** (선택적 - Upstash 무료 티어)

### Infra
- **Frontend**: Vercel (무료)
- **Backend**: AWS EC2 (t3a.small or t3.micro)
- **DB**: Supabase (무료)
- **DNS/Proxy**: Cloudflare (무료)
- **CI/CD**: GitHub Actions

---

## 📅 12주 개발 일정

### Week 1: 설계 및 환경 구축 (10~15h)

#### 목표
- 프로젝트 요구사항 정리
- ERD 설계
- API 명세서 작성
- 개발 환경 세팅

#### 작업 리스트

**Day 1 (3h)**: 요구사항 정의
- [ ] 기능 목록 작성 (MVP vs 2차)
- [ ] 유저 스토리 작성 (5~8개)
- [ ] 화면 구성 스케치 (손그림 OK)

**Day 2 (3h)**: 데이터 모델 설계
- [ ] ERD 작성 (User, Project, Endpoint, CheckResult, AlertConfig)
- [ ] 테이블 관계 정의
- [ ] 인덱스 계획

**Day 3 (3h)**: API 명세 초안
- [ ] RESTful API 엔드포인트 정의 (20~30개)
- [ ] 요청/응답 DTO 구조 설계
- [ ] Swagger 문서 구조 계획

**Day 4 (3h)**: 인프라 세팅
- [ ] GitHub 레포지토리 생성 (frontend/backend 분리 or 모노레포)
- [ ] EC2 인스턴스 생성 (t3.micro)
- [ ] Nginx 설치 및 기본 설정
- [ ] Supabase 계정 생성 및 DB 생성

**Day 5 (3h)**: 로컬 개발 환경
- [ ] Spring Boot 프로젝트 생성 (Spring Initializr)
- [ ] Next.js 프로젝트 생성
- [ ] Docker Compose 작성 (PostgreSQL, Redis)
- [ ] Git flow 전략 수립 (main, develop, feature/*)

#### 완료 기준 (Week 1)
- ✅ ERD 다이어그램 완성
- ✅ API 명세서 Markdown 완성
- ✅ `/actuator/health` 로컬 실행 확인
- ✅ Next.js 기본 페이지 로컬 실행 확인

---

### Week 2: 백엔드 기초 및 인증 (12~15h)

#### 목표
- Spring Boot 프로젝트 구조 확립
- JWT 기반 인증 구현
- 회원가입/로그인 API 완성

#### 작업 리스트

**Day 1 (3h)**: 프로젝트 구조 세팅
- [ ] 패키지 구조 설계 (domain, api, infra, config, global)
- [ ] 공통 응답 포맷 (ApiResponse<T>)
- [ ] 전역 예외 처리 (@ControllerAdvice)
- [ ] application.yml 프로파일 분리 (local, dev, prod)

**Day 2 (3h)**: DB 연결 및 User 엔티티
- [ ] Flyway 설정
- [ ] V1__init.sql 작성 (User 테이블)
- [ ] User 엔티티 작성 (JPA)
- [ ] UserRepository 작성

**Day 3 (3h)**: Spring Security 설정
- [ ] SecurityFilterChain 구성
- [ ] PasswordEncoder 빈 등록
- [ ] CORS 설정 (Vercel 도메인 허용)
- [ ] 기본 Security 테스트

**Day 4 (3h)**: JWT 구현
- [ ] JWT 유틸리티 클래스 (생성, 검증, 파싱)
- [ ] JwtAuthenticationFilter 작성
- [ ] Access Token / Refresh Token 전략
- [ ] Token DTO 작성

**Day 5 (3h)**: 회원가입/로그인 API
- [ ] POST /auth/register 구현
- [ ] POST /auth/login 구현
- [ ] POST /auth/refresh 구현
- [ ] POST /auth/logout 구현 (Refresh Token 무효화)

#### 완료 기준 (Week 2)
- ✅ Postman으로 회원가입 → 로그인 → JWT 발급 테스트 성공
- ✅ Swagger UI에서 인증 플로우 확인
- ✅ 보호된 엔드포인트 접근 시 401 응답 확인

---

### Week 3: 프로젝트/엔드포인트 CRUD (14~16h)

#### 목표
- Project 도메인 구현
- Endpoint 도메인 구현
- 기본 CRUD API 완성

#### 작업 리스트

**Day 1 (3h)**: Project 엔티티 및 API
- [ ] Flyway V2: Project 테이블
- [ ] Project 엔티티, DTO, Repository
- [ ] GET /projects (내 프로젝트 목록)
- [ ] POST /projects (생성)

**Day 2 (3h)**: Project CRUD 완성
- [ ] GET /projects/{id}
- [ ] PUT /projects/{id}
- [ ] DELETE /projects/{id}
- [ ] 권한 검증 (본인 프로젝트만 수정/삭제)

**Day 3 (3h)**: Endpoint 엔티티
- [ ] Flyway V3: Endpoint 테이블
- [ ] Endpoint 엔티티 (URL, method, headers, body, expectedStatus, checkInterval 등)
- [ ] EndpointRepository

**Day 4 (3h)**: Endpoint CRUD (1)
- [ ] POST /projects/{projectId}/endpoints (엔드포인트 등록)
- [ ] GET /projects/{projectId}/endpoints (목록)
- [ ] Headers JSONB 처리 (PostgreSQL)

**Day 5 (3h)**: Endpoint CRUD (2)
- [ ] GET /endpoints/{id}
- [ ] PUT /endpoints/{id}
- [ ] DELETE /endpoints/{id}
- [ ] PATCH /endpoints/{id}/toggle (활성화/비활성화)

#### 완료 기준 (Week 3)
- ✅ 프로젝트 생성 → 엔드포인트 등록 → 조회 E2E 테스트 성공
- ✅ Swagger로 모든 CRUD 동작 확인
- ✅ 다른 사용자의 데이터 접근 시 403 응답

---

### Week 4: HTTP 체크 로직 구현 (15~18h)

#### 목표
- 외부 API를 호출하는 HttpChecker 서비스 구현
- 체크 결과를 DB에 저장
- 수동 테스트 API 구현

#### 작업 리스트

**Day 1 (3h)**: CheckResult 엔티티
- [ ] Flyway V4: CheckResult 테이블 (시계열 데이터)
- [ ] CheckResult 엔티티 (endpointId, status, statusCode, responseTime, errorMessage, checkedAt)
- [ ] CheckResultRepository
- [ ] 인덱스 설정 (endpointId + checkedAt)

**Day 2 (3h)**: RestTemplate 설정
- [ ] RestTemplate 빈 구성 (Timeout, ConnectionPool)
- [ ] HttpChecker 서비스 스켈레톤
- [ ] HTTP 요청 빌더 (Headers, Body 파싱)

**Day 3 (4h)**: HttpChecker 핵심 로직
- [ ] `CheckResult check(Endpoint endpoint)` 메서드
- [ ] 응답 시간 측정 (startTime, endTime)
- [ ] 상태 코드 검증
- [ ] 예외 처리 (Timeout, Connection 실패 등)

**Day 4 (3h)**: 수동 테스트 API
- [ ] POST /endpoints/{id}/test (즉시 체크 실행)
- [ ] 실행 결과 즉시 반환 (동기)
- [ ] 결과를 DB에도 저장

**Day 5 (3h)**: 검증 로직 강화
- [ ] 최대 응답 시간 검증
- [ ] 응답 본문 기본 검증 (JSON 유효성)
- [ ] 재시도 로직 (1회)
- [ ] 단위 테스트 작성

#### 완료 기준 (Week 4)
- ✅ 실제 외부 API(예: https://httpbin.org) 호출 테스트 성공
- ✅ 성공/실패 케이스 모두 정상 처리
- ✅ DB에 CheckResult 저장 확인

---

### Week 5: 스케줄러 구현 (15~18h)

#### 목표
- Spring Scheduler로 자동 헬스체크 구현
- 체크 주기별 실행 로직
- 병렬 처리 및 성능 최적화

#### 작업 리스트

**Day 1 (3h)**: Scheduler 기본 설정
- [ ] @EnableScheduling 활성화
- [ ] HealthCheckScheduler 클래스 생성
- [ ] 1분마다 실행되는 메서드 작성

**Day 2 (3h)**: 체크 대상 조회 로직
- [ ] "지금 체크할 시간이 된 엔드포인트" 조회 쿼리
- [ ] lastCheckedAt + checkInterval 계산
- [ ] isActive = true 필터링

**Day 3 (4h)**: 병렬 처리
- [ ] CompletableFuture 또는 parallelStream 활용
- [ ] ThreadPoolTaskExecutor 설정
- [ ] 동시 실행 수 제한 (예: 10개)

**Day 4 (3h)**: 체크 완료 후 처리
- [ ] CheckResult 저장
- [ ] Endpoint의 lastCheckedAt 업데이트
- [ ] 에러 로깅

**Day 5 (3h)**: 성능 최적화 및 테스트
- [ ] Batch Insert 적용 (CheckResult)
- [ ] 느린 API에 대한 Timeout 동작 확인
- [ ] 로그로 실행 시간 측정
- [ ] 통합 테스트 (실제 스케줄러 동작)

#### 완료 기준 (Week 5)
- ✅ 1분마다 자동으로 엔드포인트 체크 실행
- ✅ 여러 엔드포인트가 병렬로 체크됨
- ✅ 로그에서 체크 결과 확인 가능

---

### Week 6: 통계 API 구현 (14~16h)

#### 목표
- 엔드포인트별 통계 조회 API
- 성공률, 평균 응답시간 계산
- 시간대별 데이터 집계

#### 작업 리스트

**Day 1 (3h)**: 기본 통계 쿼리
- [ ] GET /endpoints/{id}/stats 엔드포인트
- [ ] 최근 24시간 성공률 계산
- [ ] 평균 응답시간 계산
- [ ] 총 체크 횟수

**Day 2 (3h)**: 시간대별 집계
- [ ] 시간별 평균 응답시간 (GROUP BY HOUR)
- [ ] PostgreSQL date_trunc 활용
- [ ] DTO 매핑 (시간, 평균값)

**Day 3 (4h)**: 프로젝트 전체 통계
- [ ] GET /projects/{id}/stats
- [ ] 프로젝트 내 모든 엔드포인트 통계 합산
- [ ] UP/DOWN 개수
- [ ] 전체 평균 응답시간

**Day 4 (3h)**: 최근 체크 결과 조회
- [ ] GET /endpoints/{id}/recent-checks?limit=20
- [ ] 페이징 처리
- [ ] 정렬 (최신순)

**Day 5 (3h)**: 성능 최적화
- [ ] 쿼리 성능 측정 (EXPLAIN ANALYZE)
- [ ] 인덱스 추가 (필요 시)
- [ ] Redis 캐싱 적용 (통계는 5분 캐시)

#### 완료 기준 (Week 6)
- ✅ 통계 API로 실시간 데이터 확인
- ✅ 응답 속도 < 500ms
- ✅ Swagger로 모든 통계 API 테스트 완료

---

### Week 7: 알림 시스템 구현 (16~18h)

#### 목표
- 이메일 알림 구현
- Slack Webhook 알림 구현
- 알림 조건 설정 (연속 N회 실패)

#### 작업 리스트

**Day 1 (3h)**: AlertConfig 엔티티
- [ ] Flyway V5: AlertConfig 테이블
- [ ] AlertConfig 엔티티 (type, target, threshold)
- [ ] AlertConfigRepository

**Day 2 (3h)**: AlertConfig CRUD API
- [ ] GET /alerts (내 알림 설정 목록)
- [ ] POST /alerts (알림 추가)
- [ ] DELETE /alerts/{id}

**Day 3 (4h)**: 이메일 알림
- [ ] JavaMailSender 설정
- [ ] EmailService 작성
- [ ] 알림 템플릿 작성 (HTML)
- [ ] 테스트 발송

**Day 4 (3h)**: Slack Webhook 알림
- [ ] SlackService 작성
- [ ] Webhook URL 호출
- [ ] 메시지 포맷 (JSON)
- [ ] 테스트 발송

**Day 5 (4h)**: 알림 트리거 로직
- [ ] AlertService.checkAndNotify() 메서드
- [ ] 최근 N회 체크 결과 조회
- [ ] 연속 실패 횟수 계산
- [ ] 임계값 초과 시 알림 발송
- [ ] 중복 알림 방지 (Redis 사용)

#### 완료 기준 (Week 7)
- ✅ 실제 API 장애 시 이메일 수신 확인
- ✅ Slack 채널에 알림 도착 확인
- ✅ 중복 알림 없이 1회만 발송됨

---

### Week 8: 프론트엔드 기본 구조 (12~15h)

#### 목표
- Next.js 프로젝트 초기 세팅
- 인증 플로우 구현
- 레이아웃 및 네비게이션

#### 작업 리스트

**Day 1 (3h)**: Next.js 세팅
- [ ] create-next-app 실행
- [ ] Tailwind CSS 설정
- [ ] shadcn/ui 초기화
- [ ] 폴더 구조 설계 (app, components, lib, types)

**Day 2 (3h)**: API 클라이언트
- [ ] axios 설정 (baseURL, interceptors)
- [ ] JWT 토큰 자동 주입
- [ ] 401 응답 시 자동 로그아웃
- [ ] TypeScript 타입 정의

**Day 3 (3h)**: 인증 페이지
- [ ] /login 페이지
- [ ] /register 페이지
- [ ] React Hook Form 사용
- [ ] 로그인 후 대시보드로 리다이렉트

**Day 4 (3h)**: 레이아웃
- [ ] 사이드바 컴포넌트 (shadcn/ui)
- [ ] 헤더 (로그아웃 버튼)
- [ ] 보호된 라우트 (미들웨어)
- [ ] 로딩 스피너

**Day 5 (3h)**: 전역 상태 관리
- [ ] Zustand 또는 Context API 설정
- [ ] User 정보 저장
- [ ] Token 관리 (localStorage)

#### 완료 기준 (Week 8)
- ✅ 로그인 → 대시보드 진입 성공
- ✅ 로그아웃 동작 확인
- ✅ 보호된 페이지 접근 시 리다이렉트

---

### Week 9: 대시보드 및 목록 화면 (14~16h)

#### 목표
- 메인 대시보드 구현
- 프로젝트/엔드포인트 목록 표시

#### 작업 리스트

**Day 1 (3h)**: 대시보드 레이아웃
- [ ] 전체 통계 카드 (총 엔드포인트, UP/DOWN)
- [ ] 평균 응답시간 표시
- [ ] shadcn/ui Card 컴포넌트 사용

**Day 2 (4h)**: 차트 연동
- [ ] Chart.js 또는 Recharts 설치
- [ ] 평균 응답시간 라인 차트
- [ ] 최근 24시간 데이터
- [ ] 반응형 디자인

**Day 3 (3h)**: 최근 알림 목록
- [ ] 최근 알림 테이블
- [ ] 시간 포맷 (moment.js or date-fns)
- [ ] 상태별 아이콘 (성공/실패)

**Day 4 (3h)**: 프로젝트 목록
- [ ] /projects 페이지
- [ ] GET /projects API 호출
- [ ] 카드 형태 또는 테이블
- [ ] [+ 새 프로젝트] 버튼

**Day 5 (3h)**: 엔드포인트 목록
- [ ] /projects/[id]/endpoints 페이지
- [ ] 환경별 그룹핑 (DEV/STAGING/PROD)
- [ ] 상태 뱃지 (UP/DOWN)
- [ ] 평균 응답시간 표시

#### 완료 기준 (Week 9)
- ✅ 대시보드에서 실시간 통계 확인
- ✅ 차트로 데이터 시각화
- ✅ 프로젝트/엔드포인트 목록 표시

---

### Week 10: 엔드포인트 등록/수정 폼 (14~16h)

#### 목표
- 엔드포인트 등록 폼 구현
- 수정/삭제 기능
- 상세 통계 페이지

#### 작업 리스트

**Day 1 (4h)**: 등록 폼 UI
- [ ] /projects/[id]/endpoints/new 페이지
- [ ] React Hook Form 설정
- [ ] 입력 필드: URL, Method, Headers, Body, 예상 상태코드, 체크 주기
- [ ] Headers 동적 추가/삭제

**Day 2 (3h)**: 등록 API 연동
- [ ] POST /projects/{id}/endpoints 호출
- [ ] Validation 에러 처리
- [ ] 성공 시 목록으로 리다이렉트
- [ ] Toast 알림

**Day 3 (3h)**: 수정 폼
- [ ] /endpoints/[id]/edit 페이지
- [ ] GET /endpoints/{id}로 기존 데이터 로드
- [ ] 폼 초기값 설정
- [ ] PUT /endpoints/{id} 호출

**Day 4 (3h)**: 삭제 및 활성화 토글
- [ ] 삭제 버튼 (확인 모달)
- [ ] DELETE /endpoints/{id}
- [ ] 활성화/비활성화 토글 스위치
- [ ] PATCH /endpoints/{id}/toggle

**Day 5 (3h)**: 상세 통계 페이지
- [ ] /endpoints/[id] 페이지
- [ ] 성공률 도넛 차트
- [ ] 응답시간 추이 라인 차트
- [ ] 최근 체크 결과 테이블

#### 완료 기준 (Week 10)
- ✅ 엔드포인트 등록 → 목록 확인 → 수정 → 삭제 E2E
- ✅ 상세 통계 페이지에서 차트 확인
- ✅ 모든 폼 Validation 동작

---

### Week 11: 테스트 및 리팩터링 (16~18h)

#### 목표
- 백엔드 테스트 작성
- 코드 리팩터링
- 성능 최적화

#### 작업 리스트

**Day 1 (4h)**: 백엔드 단위 테스트
- [ ] Service 계층 테스트 (@Mock)
- [ ] HttpChecker 테스트
- [ ] AlertService 테스트
- [ ] 커버리지 50% 이상

**Day 2 (3h)**: 통합 테스트
- [ ] @SpringBootTest
- [ ] API 엔드포인트 테스트
- [ ] TestContainers (PostgreSQL)
- [ ] 주요 플로우 테스트

**Day 3 (3h)**: 프론트엔드 리팩터링
- [ ] 컴포넌트 분리 (공통 컴포넌트)
- [ ] 커스텀 훅 추출 (useAuth, useApi)
- [ ] 에러 처리 통일
- [ ] 코드 정리

**Day 4 (3h)**: 성능 최적화
- [ ] 느린 쿼리 개선 (EXPLAIN ANALYZE)
- [ ] N+1 문제 해결 (fetch join)
- [ ] Redis 캐싱 확대 적용
- [ ] API 응답 시간 측정

**Day 5 (3h)**: 보안 강화
- [ ] SQL Injection 방어 확인
- [ ] XSS 방어 (입력 검증)
- [ ] CSRF 토큰 (필요 시)
- [ ] Rate Limiting (Nginx)

#### 완료 기준 (Week 11)
- ✅ 테스트 커버리지 50% 이상
- ✅ 주요 API 응답 시간 < 300ms
- ✅ 보안 체크리스트 완료

---

### Week 12: 배포 및 문서화 (14~16h)

#### 목표
- 프로덕션 배포
- CI/CD 구축
- README 및 문서 작성

#### 작업 리스트

**Day 1 (3h)**: 프론트엔드 배포
- [ ] Vercel 프로젝트 연결
- [ ] 환경변수 설정 (NEXT_PUBLIC_API_URL)
- [ ] 도메인 연결 (선택)
- [ ] 배포 확인

**Day 2 (4h)**: 백엔드 배포
- [ ] EC2 Docker 이미지 배포
- [ ] Nginx 프록시 설정
- [ ] Let's Encrypt TLS 인증서
- [ ] 환경변수 설정 (.env)

**Day 3 (3h)**: CI/CD
- [ ] GitHub Actions 워크플로우 작성
- [ ] Backend: 테스트 → 빌드 → Docker 푸시 → EC2 재시작
- [ ] Frontend: Vercel 자동 배포
- [ ] main 브랜치 푸시 시 자동 배포

**Day 4 (3h)**: 모니터링 설정
- [ ] Spring Actuator 활성화
- [ ] CloudWatch 에이전트 (또는 Grafana Cloud)
- [ ] 에러 알림 설정
- [ ] 헬스체크 엔드포인트

**Day 5 (3h)**: 문서화
- [ ] README.md 작성
  - 프로젝트 소개
  - 기술 스택
  - 아키텍처 다이어그램
  - ERD
  - API 문서 링크
  - 실행 방법
  - 배포 URL
  - 스크린샷/GIF
- [ ] CONTRIBUTING.md (선택)
- [ ] 회고 작성

#### 완료 기준 (Week 12)
- ✅ 프로덕션 URL 접속 가능
- ✅ CI/CD 자동 배포 동작
- ✅ README 완성
- ✅ 포트폴리오에 추가 가능한 상태

---

## 📊 주간 회고 템플릿

매주 금요일 또는 일요일에 작성:

```markdown
## Week N 회고 (YYYY-MM-DD)

### ✅ 완료한 것
- 
- 

### ⚠️ 어려웠던 점
- 
- 

### 💡 배운 것
- 
- 

### 📋 다음 주 목표
- 
- 

### ⏱️ 실제 투입 시간
- 평일: h
- 주말: h
- 합계: h
```

---

## 🎯 일일 체크리스트 템플릿

매일 작업 전에 체크:

```markdown
## 날짜: YYYY-MM-DD

### 🎯 오늘의 목표 (1~3개)
1. [ ] 
2. [ ] 
3. [ ] 

### ⏰ 작업 시간
- 시작: 
- 종료: 
- 순수 작업 시간: 

### ✅ 완료
- 

### 📝 작업 로그
- 

### 🚧 막힌 점 / 질문
- 

### 💭 내일 할 일
- 
```

---

## 🔥 중요 체크포인트

### Week 4 (1개월차)
- ✅ 백엔드 CRUD + 헬스체크 동작
- ✅ Swagger로 모든 API 테스트 가능

### Week 8 (2개월차)
- ✅ 스케줄러 + 알림 동작
- ✅ 프론트 기본 화면 완성

### Week 12 (3개월차)
- ✅ 배포 완료
- ✅ 실제 본인 프로젝트 모니터링 중
- ✅ 포트폴리오 등록

---

## 💡 Tips

### 시간 관리
- 퇴근 후 바로 시작 (저녁 식사 후 20:00~23:00)
- 주말은 오전/오후 나눠서 (10:00~13:00, 14:00~17:00)
- 하루 목표를 3개 이하로 제한

### 개발 효율
- 매일 GitHub commit (최소 1개)
- 막히면 30분 룰: 30분 넘으면 질문/검색
- Copilot/ChatGPT 적극 활용
- 완벽주의 버리기 (80% 완성도로 넘어가기)

### 동기 부여
- 주간 데모: 매주 일요일 완성된 것 시연
- 진척도 시각화: Notion 칸반 보드
- 베타 유저: 주변 개발자에게 사용 요청

---

## 📚 참고 자료

### Spring Boot
- Spring 공식 문서: https://spring.io/guides
- Baeldung: https://www.baeldung.com/
- Spring Security JWT: https://jwt.io/

### Next.js
- Next.js 공식 문서: https://nextjs.org/docs
- shadcn/ui: https://ui.shadcn.com/
- React Hook Form: https://react-hook-form.com/

### 인프라
- AWS EC2: https://aws.amazon.com/ec2/
- Supabase: https://supabase.com/docs
- Vercel: https://vercel.com/docs

---

## 🎉 완성 후 다음 단계

### MVP 이후 추가 기능
1. WebSocket 실시간 대시보드
2. Response Body JSON 검증 (특정 필드 체크)
3. 팀 협업 기능 (멀티 유저)
4. 공개 Status Page (외부 공유용)
5. API 호출 로그 상세 조회
6. Slack Bot 연동
7. 모바일 앱 (React Native)

### 포트폴리오 활용
- 이력서에 프로젝트 링크 포함
- GitHub README에 데모 GIF
- 블로그 포스팅 (개발 과정 회고)
- 오픈소스 공개 (선택)

---

**작성일**: 2024-11-06  
**예상 완료일**: 2025-01-29 (12주 후)  
**마지막 업데이트**: 2024-11-06
