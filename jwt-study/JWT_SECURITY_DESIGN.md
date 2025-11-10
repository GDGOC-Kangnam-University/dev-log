# JWT 인증 시스템 설계 문서

## 1. JWT의 보안적 특성

### 1.1 이중 토큰 전략의 필요성

JWT 인증 시스템은 Access Token과 Refresh Token을 분리하여 보안성과 사용성의 균형을 맞춥니다.

**Access Token**
- 짧은 유효기간 (15분)
- 매 API 요청마다 전송
- 탈취 시 피해를 최소화하기 위해 빠르게 만료

**Refresh Token**
- 긴 유효기간 (7일)
- 토큰 갱신 시에만 사용
- DB에 저장하여 즉시 무효화 가능

### 1.2 주요 보안 위협과 대응

**XSS (Cross-Site Scripting) 대응**
- Access Token: 클라이언트 메모리에만 저장 (LocalStorage 사용 금지)
- Refresh Token: HttpOnly Cookie로 JavaScript 접근 차단

**CSRF (Cross-Site Request Forgery) 대응**
- SameSite=Strict 쿠키 속성 설정
- Refresh Token 엔드포인트를 /api/auth 경로로 제한

**Token Replay Attack 대응**
- Access Token: 짧은 만료시간
- Refresh Token: RTR(Refresh Token Rotation) 패턴으로 한 번 사용 후 폐기

**Man-in-the-Middle Attack 대응**
- HTTPS 필수 사용
- Secure 쿠키 플래그 설정

### 1.3 Refresh Token Rotation (RTR) 패턴

RTR은 Refresh Token을 한 번만 사용하고 폐기하여 재사용 공격을 방지합니다.

동작 방식:
1. 클라이언트가 Refresh Token으로 갱신 요청
2. 서버는 DB에서 토큰이 이미 사용되었는지 확인
3. 유효하면 새로운 Access Token + Refresh Token 발급
4. 기존 Refresh Token은 "사용됨"으로 표시
5. 재사용 감지 시 해당 사용자의 모든 토큰 삭제

## 2. 요청/응답 형식

### 2.1 로그인

**요청:**
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "user123",
  "password": "SecurePass123!"
}
```

**응답:**
```http
HTTP/1.1 200 OK
Set-Cookie: refreshToken=eyJhbG...; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800

{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

- Refresh Token은 응답 본문이 아닌 Set-Cookie 헤더로 전달
- Access Token은 응답 본문에 포함
- 이 분리가 XSS 공격으로부터 Refresh Token을 보호

### 2.2 토큰 갱신

**요청:**
```http
POST /api/auth/refresh
Cookie: refreshToken=eyJhbG...
```

**응답:**
```http
HTTP/1.1 200 OK
Set-Cookie: refreshToken=newEyJh...; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800

{
  "accessToken": "newEyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

RTR 동작: 기존 토큰 무효화 + 새 토큰 쌍 발급

### 2.3 보호된 API 호출

**요청:**
```http
GET /api/protected/resource
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**성공 응답:**
```json
{
  "data": "protected resource"
}
```

**토큰 만료 시:**
```json
{
  "error": "token_expired",
  "message": "토큰이 만료되었습니다"
}
```

클라이언트는 401 응답 시 자동으로 /api/auth/refresh를 호출하여 새 토큰을 획득한 후 원래 요청을 재시도합니다.

## 3. 계층별 작업 흐름

### 3.1 로그인 처리 흐름

```
[1] AuthController.login()
    역할: HTTP 요청 받기, 쿠키 설정
    입력: LoginRequest (username, password)
    처리: AuthService 호출

[2] AuthService.authenticate()
    역할: 사용자 인증
    처리: DB에서 사용자 조회, 비밀번호 검증
    출력: User 객체

[3] TokenService.issueTokenPair()
    역할: 토큰 발급 및 DB 저장
    처리:
        JwtTokenProvider.generateAccessToken() 호출
        JwtTokenProvider.generateRefreshToken() 호출
        Refresh Token을 SHA-256 해시로 DB 저장
    출력: Access Token, Refresh Token

[4] AuthController (계속)
    Refresh Token을 HttpOnly Cookie로 설정
    Access Token을 JSON 응답으로 반환
```

### 3.2 보호된 API 호출 흐름

```
[1] JwtAuthenticationFilter.doFilterInternal()
    역할: 모든 요청을 가로채서 토큰 검증
    처리:
        Authorization 헤더에서 토큰 추출
        JwtTokenProvider.getUserId() 호출
        SecurityContext에 인증 정보 설정
    결과: 인증 성공 시 다음 필터로, 실패 시 401/403

[2] Controller
    역할: 비즈니스 로직 실행
    처리: SecurityContext에서 현재 사용자 정보 추출
    출력: API 응답
```

### 3.3 토큰 갱신 흐름 (RTR)

```
[1] AuthController.refresh()
    역할: Cookie에서 Refresh Token 추출
    처리: TokenService 호출

[2] TokenService.rotateRefreshToken()
    역할: RTR 패턴 구현
    처리:
        1) JwtTokenProvider로 JWT 파싱 및 서명 검증
        2) JWT Claims에서 userId 추출
        3) tokenHash 계산 (SHA-256)
        4) DB 조회 (SELECT FOR UPDATE로 행잠금)
        5) 재사용 여부 확인
           - 이미 사용됨 → 해당 사용자의 모든 토큰 삭제 + 예외
        6) 만료 여부 확인 (Idle 7일)
           - 만료됨 → 예외 발생
        7) 기존 토큰을 "사용됨"으로 표시
        8) JwtTokenProvider로 새 토큰 쌍 발급
    출력: 새 Access Token, 새 Refresh Token

[3] AuthController (계속)
    새 Refresh Token을 Cookie로 설정
    새 Access Token을 JSON 응답
```

## 4. 핵심 클래스별 책임

### 4.1 AuthController
- HTTP 요청/응답 처리
- Cookie 설정
- DTO 변환
- Service 호출 및 결과 반환

### 4.2 AuthService
- 사용자 인증 (비밀번호 검증)
- 회원가입 처리
- 비즈니스 규칙 적용

### 4.3 TokenService
- 토큰 발급 및 관리
- RTR 패턴 구현
- Refresh Token DB 저장/검증
- 재사용 탐지 및 보안 조치

### 4.4 JwtTokenProvider
- JWT 생성 (Access, Refresh)
- JWT 파싱 및 서명 검증
- Claims 추출

### 4.5 JwtAuthenticationFilter
- 모든 HTTP 요청 인터셉트
- Authorization 헤더에서 토큰 추출
- 토큰 검증 (JwtTokenProvider 호출)
- SecurityContext에 인증 정보 설정

### 4.6 RefreshTokenRepository
- Refresh Token DB CRUD
- 행잠금을 통한 동시성 제어
- 사용자별 토큰 삭제

## 5. 보안 설계의 핵심 원칙

### 5.1 저장 위치 분리
- Access Token: 클라이언트 메모리 (XSS 공격 시에도 페이지 새로고침하면 사라짐)
- Refresh Token: HttpOnly Cookie (JavaScript 접근 불가)

### 5.2 전송 방식 분리
- Access Token: Authorization 헤더 (Bearer 스킴)
- Refresh Token: Cookie (특정 경로로만 제한)

### 5.3 DB 저장 전략
- Access Token: DB 저장 안 함 (Stateless, 성능 최적화)
- Refresh Token: 해시값만 DB 저장 (즉시 무효화 가능, 재사용 탐지)

## 6. 코드 리뷰 피드백 및 구조 개선

> **참고:** 이 문서(섹션 1-5, 7-8)는 권장 구조를 기준으로 작성되었습니다. 현재 프로젝트는 `util/JwtUtil.java`를 사용 중이지만, 문서에서는 권장 구조인 `security/provider/JwtTokenProvider.java`로 설명합니다.

### 6.1 현재 프로젝트의 구조적 문제

**현재 프로젝트 구조:**
```
src/main/java/com/example/jwt_study/
├── util/
│   └── JwtUtil.java          ← 문제: 의존성이 있는 클래스가 util에 위치
├── service/
│   └── TokenService.java     ← 문제: 보안 로직이 일반 service에 위치
└── security/
    └── JwtAuthenticationFilter.java  ← 문제: 플랫 구조
```

### 6.2 코드 리뷰 피드백

1. **refreshTokenAbsoluteExpiry 미사용**
   - `JwtProperties`에 정의되어 있지만 실제 로직에서 사용되지 않음
   - **개선:** 현재 프로젝트에 불필요하므로 제거

2. **validateTokenType() 미호출**
   - `JwtUtil.validateTokenType()` 메서드가 존재하지만 실제로 호출되지 않음
   - **개선:** 복잡도 대비 실효성 낮음. 메서드 삭제

3. **JwtUtil의 위치 및 명명 문제**
   - **현재:** `util/JwtUtil.java` - JwtProperties에 의존하는 Component가 util 패키지에 위치
   - **문제:** Util은 static 메서드 모음을 의미하지만 실제로는 @Component 빈
   - **개선:** `security/provider/JwtTokenProvider.java`로 이동 및 리네임

4. **TokenService 위치 문제**
   - **현재:** `service/TokenService.java`
   - **문제:** 토큰 관리는 보안 도메인의 책임인데 일반 service 패키지에 위치
   - **개선:** `security/service/TokenService.java`로 이동

5. **예외 처리 구조**
   - **현재:** 여러 개의 Exception 클래스 (InvalidTokenException, TokenExpiredException 등)
   - **개선:** Enum 기반 ErrorCode + 단일 BusinessException으로 통합

6. **DTO 변환 방식**
   - **현재:** Controller에서 직접 `new UserResponse()` 사용
   - **개선:** 정적 팩토리 메서드 `UserResponse.from(user)` 패턴 적용

## 7. 패키지 구조 설계

일반적인 JWT패키지 구조를 검색해 정리해 보았습니다.

### 7.1 전체 패키지 구조

```
com.example.jwt_study
├── JwtStudyApplication.java
│
├── config/
│   ├── SecurityConfig.java
│   └── JwtProperties.java
│
├── security/
│   ├── filter/
│   │   └── JwtAuthenticationFilter.java
│   ├── provider/
│   │   └── JwtTokenProvider.java
│   ├── handler/
│   │   └── JwtAuthenticationEntryPoint.java
│   └── service/
│       └── TokenService.java
│
├── controller/
│   ├── AuthController.java
│   └── UserController.java
│
├── service/
│   └── AuthService.java
│
├── repository/
│   ├── UserRepository.java
│   └── RefreshTokenRepository.java
│
├── domain/
│   ├── User.java
│   └── RefreshToken.java
│
├── dto/
│   ├── request/
│   │   ├── LoginRequest.java
│   │   └── RegisterRequest.java
│   └── response/
│       ├── TokenResponse.java
│       ├── UserResponse.java
│       └── ErrorResponse.java
│
└── exception/
    ├── GlobalExceptionHandler.java
    ├── TokenException.java
    ├── TokenErrorType.java
    └── DuplicateUsernameException.java
```

### 7.2 각 패키지의 역할과 설계 의도

**config 패키지**
- Spring 설정 클래스 관리
- `SecurityConfig`: Spring Security 전역 설정, 필터 체인 구성
- `JwtProperties`: application.yml의 JWT 설정값 바인딩

**security 패키지**
JWT 인증과 관련된 모든 보안 컴포넌트를 한 곳에 응집시킵니다. 하위 패키지로 역할을 명확히 구분합니다.

- `filter/`: HTTP 요청을 가로채는 필터
   - `JwtAuthenticationFilter`: Authorization 헤더에서 토큰 추출 및 검증

- `provider/`: 기술적 구현을 캡슐화하는 Provider
   - `JwtTokenProvider`: JWT 생성, 파싱, 서명 검증 담당

- `handler/`: Spring Security의 예외 처리 핸들러
   - `JwtAuthenticationEntryPoint`: 인증 실패 시 401 응답 커스터마이징

- `service/`: 보안 도메인의 비즈니스 로직
   - `TokenService`: Refresh Token RTR, DB 저장/검증

**설계 근거:**
보안 관련 코드를 한 패키지에 모으면 다음 장점이 있습니다.
- 코드 탐색 용이성: JWT 관련 코드를 찾을 때 security 패키지만 확인
- 변경 영향 범위 최소화: 인증 방식 변경 시 security 패키지만 수정
- 확장성: OAuth2, SAML 등 추가 인증 방식 도입 시 동일한 구조 적용 가능

**controller 패키지**
HTTP 엔드포인트를 제공하는 Controller 클래스들을 배치합니다.
- `AuthController`: 로그인, 회원가입, 토큰 갱신, 로그아웃
- `UserController`: 사용자 정보 조회 등 비즈니스 API

**service 패키지**
비즈니스 로직을 담당하는 Service 클래스들을 배치합니다.
- `AuthService`: 사용자 인증, 회원가입 등 인증 도메인 로직

**중요:** `TokenService`는 service가 아닌 security/service에 위치합니다. 토큰 관리는 보안 영역의 책임이므로 분리합니다.

**repository 패키지**
데이터베이스 접근을 담당하는 JPA Repository 인터페이스들을 배치합니다.

**domain 패키지**
JPA Entity 클래스들을 배치합니다. entity 또는 model이라는 이름도 사용 가능하지만, domain이 도메인 주도 설계(DDD) 관점에서 더 명확합니다.

**dto 패키지**
Data Transfer Object를 request와 response로 구분하여 배치합니다.

- `request/`: 클라이언트로부터 받는 요청 DTO
- `response/`: 클라이언트에게 반환하는 응답 DTO

**설계 근거:**
패키지 이름만으로 데이터 흐름 방향을 파악할 수 있어 가독성이 향상됩니다.

**exception 패키지**
애플리케이션 전역에서 사용하는 예외 클래스와 예외 처리 핸들러를 배치합니다.

- `GlobalExceptionHandler`: @RestControllerAdvice로 전역 예외 처리
- `TokenException`: 토큰 관련 예외 통합 클래스
- `TokenErrorType`: 토큰 에러 타입을 정의하는 Enum

**설계 근거:**
예외는 여러 계층에서 발생하고 처리되므로 특정 도메인에 종속시키지 않고 최상위 패키지에 배치합니다.





## 8. 결론

JWT 인증 시스템의 보안은 다음 원칙으로 구현됩니다:

1. **이중 토큰 전략**: Access는 짧게, Refresh는 길게
2. **저장 위치 분리**: Access는 메모리, Refresh는 HttpOnly Cookie
3. **RTR 패턴**: Refresh Token 재사용 탐지 및 세션 무효화
4. **계층별 책임 분리**: Controller, Service, Provider 각각의 명확한 역할
