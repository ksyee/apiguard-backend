package com.apiguard.backend.global.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {
    
    private final String secret; // 비밀키 (application.yml에서 가져옴)
    private final long accessExpiration; // Access Token 수명(1시간)
    private final long refreshExpiration; // Refresh Token 수명(14일)
    private SecretKey key; // 실제 암호화에 쓸 키 객체
    
    public JwtTokenProvider( // 생성자에서 @Value로 값을 주입받음.
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-expiration}") long accessExpiration,
        @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        
        this.secret = secret;
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }
    
    @PostConstruct
    protected void init() {
        // 비밀키 문자열을 SecretKey 객체로 변환
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    
    // Access Token 생성
    /*
     * subject(email): 이 토큰의 주인은 누구인가? (이메일)
     * add("role", role): 이 사람의 권한은 무엇인가? (USER, ADMIN 등)
     * expiration(...): 언제까지 유효한가? (현재 시간 + accessExpiration)
     * signWith(key): 위조 방지를 위해 내 비밀키로 도장을 쾅 찍음.
     */
    public String createAccessToken(String email, String role) {
        Claims claims = Jwts.claims()
            .subject(email)
            .add("role", role)
            .build();
        
        Date now = new Date();
        
        return Jwts.builder()
            .claims(claims)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + accessExpiration))
            .signWith(key)
            .compact();
    }
    
    // Refresh Token 생성
    /*
     * 재발급 쿠폰 발급
     * Access Token과 달리 role(권한) 정보는 보통 넣지 않음. (재발급 받을 때 DB에서 다시 조회하면 됨)
     * 유효 기간이 길다.
     */
    public String createRefreshToken(String email) {
        Date now = new Date();
        
        return Jwts.builder()
            .subject(email)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + refreshExpiration))
            .signWith(key)
            .compact();
    }
    
    // 토큰에서 인증 정보 추출
    /*
     * 사용자가 토큰을 들고 오면, 그 안에서 email과 role을 꺼냄
     * 이 정보를 바탕으로 스프링 시큐리티가 이해하는 Authentication 객체(통행증)를 만들어 줌
     * 이게 있어야 컨트롤러에서 @AuthenticationPrincipal로 로그인한 사용자 정보를 쓸 수 있음.
     */
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        String role = claims.get("role", String.class);
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
            new SimpleGrantedAuthority(role));
        
        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }
    
    // 토큰 유효성 검증
    /*
     * 비밀키(key)를 사용해 이 토큰이 내가 발급한 게 맞는지 확인.
     * 만약 해커가 내용을 조작했거나(SignatureException), 유효기간이 지났으면(ExpiredJwtException) 에러가 터지고 false를 반환.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        
        return false;
    }
    
    private Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
