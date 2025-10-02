## JWT 보안 특성

<aside>
💡

**헤더(Header),**

- 알고리즘 정의(alg), 타입결정(typ)

**페이로드(Payload)**

- sub, name, admin ex)

**서명(Signature)** 3가지 파트로 구성

- 헤더 영역과 페이로드 영역을 결합한 데이터를 서버 비밀키(Secret)을 통해 암호화
</aside>

- JWT의 **보안 특성**은 주로 **무결성, 인증**
    - JWT는 서명(Signature)을 통해 토큰이 변조되지 않았음을 검증할 수 있어 데이터 위변조를 방지하지만,

      페이로드(내용)는 누구나 디코딩할 수 있기 때문에 기밀성을 보장하지 않는다.

- **무상태성**(Stateless)및 **확장성**
    - 서버가 세션 정보를 저장하지 않으므로 **마이크로서비스와 분산 시스템**에서 활용하기 적합하다.
    - 세션을 저장할 필요 없이, 토큰을 통해 사용자 인증 진행한다.

## JWT 탈취 예방 방법

- **Cookie에 저장 (XSS 방어)**
    - HttpOnly 플래그로 JavaScript 접근 차단(XSS 방어)
    - 매번 Client에서 API request에 Access Token을 담는 비용이 사라짐
    - Secure 플래그로 HTTPS 연결에서만 전송
    - SameSite 속성으로 CSRF 공격 방어
- **Access/Refresh 토큰 분리 전략**
    - Refresh token만으로  Access token 재발급 불가하도록 처리
    - 액세스 토큰과 리프레시 토큰을 다른 방식으로 저장하는 하이브리드 접근법
    - 토큰 만료시간 단축
- **토큰 바인딩**
    - 토큰을 특정 요소(디바이스 지문, IP 등)에 바인딩하여 다른 환경에서 사용을 방지
- **이벤트 기반 토큰 모니터링**
    - 토큰 사용 패턴을 모니터링하여 이상 징후를 감지하고 대응
- **블랙리스트 관리**
    - 이미 발급된 토큰이더라도 특정 조건 (예: 사용자 로그아웃, 의심스러운 활동 감지 등) 하에서는 더 이상 유효하지 않도록 서버 측에서 토큰을 블랙리스트에 등록
    - 필요할 때 즉시 토큰을 무효화
    - 단점
        - 캐시 서버(Redis 등)가 필요하여 인프라 복잡성이 증가합니다.
        - 상태 저장이 필요하여 JWT의 상태 비저장(stateless) 장점이 일부 상실됩니다.
        - 모든 요청마다 블랙리스트 확인이 필요하여 성능에 영향을 줄 수 있습니다.

[[Spring] JWT 탈취 대비 - XSS, CSRF, RTR — while(true) { continue; }](https://m42-orion.tistory.com/150)

## JWT 탈취 이후 해결방안

- **RTR (Refresh Token Rotation) 사용**
    - Access token이 만료되어 Refresh token을 통해 Access token을 재발급 받을 때, Refresh token 또한 재발급을 받는 것
    - NoSQL에 해당 정보를 넣어 Refresh token의 유효성을 확인할 때 발급된 Refresh token인지 확인할 수 있어야 한다.

- **강제 재인증**
    - 위험 범위의 사용자 전원 세션 종료 → 재로그인 유도. Introspection시 “active=false”로 만들어 일괄 차단.

  [토큰 인트로스펙션 (Token introspection)은(는) 무엇인가요? · Auth Wiki](https://auth-wiki.logto.io/ko/token-introspection)

- **토큰 무효화**
    - 위험 범위 사용자들의 리프레시 토큰(그리고 가능하면 액세스 토큰)을 Revocation API로 폐기 - OAuth2 환경
    - OAuth2면 Revocation 엔드포인트로 액세스/리프레시 토큰 즉시 revok
        - 설명
            - **전제**: OAuth2/OIDC 같은 인가 서버(Authorization Server, AS)가 있다.
            - **수단**: 표준 **Revocation API**(**`/revoke`**)에 토큰을 보내면, AS가 그 토큰을 **비활성화** 처리.
            - **효과 범위**: 보낸 **그 토큰**(액세스/리프레시 각각)만 무효화.
                - 리프레시 토큰(RT)을 끊으면 **새 액세스 토큰 발급이 차단**.
                - 액세스 토큰(AT)도 함께 끊으면 **해당 AT 사용 즉시 실패**(인트로스펙션/opaque 구조일 때 바로 반영).
            - **즉시성**: 리소스 서버가 **인트로스펙션** 또는 **opaque 토큰**을 쓰면 바로 반영. 자체 검증(JWT 로컬 검증)만 하면 반영이 느릴 수 있음(만료까지 유효).
    - 순수 JWT 세션이면 jti 기반 denylist + exp까지 TTL로 막아(OWASP도 서버측 무효화전략 권고)
        - 설명
            - **전제**: 별도의 인가 서버 없이 **앱이 직접 JWT를 발급**하고, 리소스 서버가 **서명만 로컬로 검증**한다.
            - **문제**: JWT는 “자체 포함(self-contained)”이라 **발급 후 서버가 일괄 무효화할 표준 훅이 없음**.
            - **수단**: 토큰의 jti(고유 ID)를 키로 서버측 denylist(보통 Redis)에 저장하고, TTL을 토큰 만료(exp)까지 걸어둔다. 요청마다 jti가 denylist에 있으면 **거절**.
            - **효과 범위**: **등록한 jti의 토큰만** 즉시 차단. (사용자 전원의 모든 토큰을 끊으려면 해당 jti들을 전부 넣거나, 키 롤오버/클레임 정책 변경 등 추가 조치 필요.)
            - **즉시성**: 리소스 서버가 **항상 denylist를 조회**하므로 즉시 반영.

## 액세스/리프레쉬 토큰을 인증/인가 관점에서 설명

인증은 누구인지 확인하는 절차. 인가는 무엇을 할 수 있는지 결정하는 절차.

- **인증 관점**
    - **Access Token**은 **실시간 인증**을 담당하는 토큰으로, 사용자의 신원을 확인하고 각 API 요청 시마다 사용자가 누구인지 검증한다. 클라이언트가 서버에 요청을 보낼 때 Authorization 헤더에 포함되어 전송되며, 서버는 이 토큰의 서명을 검증하여 사용자를 식별함.
    - **Refresh Token**은 **재인증**을 담당하는 토큰으로, Access Token이 만료되었을 때 새로운 Access Token을 발급받기 위한 용도로만 사용된다. 이 토큰은 주로 데이터베이스에 안전하게 저장되며, 사용자가 다시 로그인하지 않고도 지속적인 인증 상태를 유지할 수 있게 해줌. 지속적인 접근을 허용할 것인지 결정.
- **인가 관점**
    - **Access Token**은 **직접적인 권한 부여**를 수행하며, 페이로드에 사용자 ID, 권한 정보, 역할 등을 포함하여 사용자가 무엇을 할 수 있는지 결정한다. 서버는 매 요청마다 이 토큰을 통해 사용자의 권한을 확인하고 적절한 리소스 접근을 허용한다.
    - **Refresh Token**은 **권한의 연장·재발급 권한**을 인가한다.
      직접적인 리소스 접근 권한은 부여하지 않는다. 오직 새로운 Access Token 발급이라는 목적으로만 사용된다.

## XSS, CSRF, CORS 해킹기법과 연관지어 JWT의 탈취 예방 방법이 어떻게 적용되는지 알아보기

- **XSS(Cross Site Scripting**)
    - 자바스크립트로 쿠키를 가로채고자 시도하는 공격(웹 사이트에 악성 스크립트를 삽입하여 사용자 세션을 탈취하는 공격)
    - **HttpOnly 속성**으로 브라우저에서 쿠키에 접속할 수 없도록 한다.
    - **CSP(Content Security Policy) 설정**으로 외부 스크립트 실행을 제한하여 XSS 공격 자체를 방지
- **CSRF(Cross-Site Request Forgery)**
    - 사용자가 인증된 상태에서 악의적인 사이트가 사용자 모르게 요청을 보내는 공격. 자동으로 전송되는 쿠키의 특성을 악용.
    - **SameSite 쿠키 속성**: SameSite=Strict 또는 Lax를 설정하여 다른 도메인에서의 쿠키 전송을 차단한다.
        - **Cookie SameSite** 속성

          Cookie의 SameSite 속성은 외부 사이트에 쿠키 전송할 범위를 설정할 수 있다.

          속성은 총 3가지가 있다.

            1. **Strict** : Cookie를 전달 할 때

          현재 페이지 도메인과 요청받는 도메인이 같아야만 쿠키가 전송

            1. **Lax** : Strict에서 a href, link href, GET Method 요청을 제외하고 Strict랑 같음(크롬 80버전부터는 SameSite Default값이 Lax로 설정된다)
            2. **None** : 도메인 검증 안함 (대신 secure 옵션이 필수로 붙어야함) SameSite를 사용하기 위해서는 프론트와 백엔드의 도메인을 맞추거나 Nginx 프록시를 사용해서 요청을 해야할 것이다.
    - **Referer 헤더 검증**: 요청의 출처를 확인하여 신뢰할 수 있는 도메인에서만 요청을 허용한다. **Origin 헤더** 검증을 함께 고려.
    - **이중 토큰 패턴**: JWT를 쿠키와 헤더에 동시에 포함하여 양쪽이 일치할 때만 인증을 허용한다. 세션 바인딩 HMAC 토큰이 있는 서명된 이중 제출 쿠키 패턴 권장.

      [교차 사이트 요청 위조 방지 - OWASP 치트 시트 시리즈](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html)

- **CORS (Cross-Origin Resource Sharing)** 오남용, 해킹기법
    - [CORS(Cross-Origin Resource Sharing) 정책과 해결 방법](https://bnzn2426.tistory.com/159)

  | 잘못된 CORS 설정 | 위험(탈취 시나리오) | 예방(권장 설정/구조) |
      | --- | --- | --- |
  | **Origin 반사(아무 Origin이나 그대로 허용)** | 공격자 사이트가 **사용자 쿠키를 포함한 요청**을 보내고, **응답(JSON의 access_token 등)**을 JS로 읽어 토큰 탈취.  | **정확 매칭 화이트리스트**만 허용(`https://app.example.com` 딱 하나), 임의 반사 금지. `Vary: Origin` 추가로 캐시 혼동 방지.  |
  | **`Access-Control-Allow-Origin: *` + `…Allow-Credentials: true`** | (조합 자체는 스펙상 금지지만) 프록시/오배포로 우회되면 **크리덴셜 요청 + 응답 읽기**가 가능해질 수 있음.  | **크리덴셜을 허용할 땐 단일 Origin만** 반환(정확 일치), 가능하면 **크리덴셜 자체를 불허**.  |
  | **너무 넓은 Allow-Methods/Headers** | 아무 출처 JS가 **민감 메서드/헤더**로 호출하고 응답을 읽어 **리프레시→액세스 교환 응답** 등을 탈취.  | **정말 필요한 메서드/헤더만** 허용, 프리플라이트에서 **Origin·메서드·헤더 교차검증**.  |
  | **`Origin: null` 허용**(file://, sandbox iframe) | 로컬 파일/임베드 뷰어 경유로 민감 응답을 읽혀 **토큰·프로필 데이터 노출**.  | `null` Origin 차단, 샌드박스·파일 스킴은 별도 경로/도메인에서만 접근.  |
    - **Origin 기반 토큰 바인딩**: JWT 페이로드에 허용된 Origin 정보를 포함하여, 다른 도메인에서 토큰 사용 시 서버에서 거부하도록 구현한다.
    - **동적 Origin 검증**: 하드코딩된 도메인 리스트 대신 데이터베이스나 설정 파일에서 허용된 Origin을 동적으로 관리하여, 필요시 즉시 차단이나 허용이 가능하도록 구현한다.
    - **모니터링 및 로깅**: CORS 위반 시도를 실시간으로 모니터링하고 로깅하여, 비정상적인 크로스 도메인 접근 시도를 탐지한다.
    - **Custom 헤더 활용**: X-Requested-With 같은 커스텀 헤더를 필수로 요구하여 모든 요청이 Preflight 검사를 거치도록 강제할 수 있다.
    - **엄격한 Origin 관리, Preflight 활용, 토큰 바인딩**