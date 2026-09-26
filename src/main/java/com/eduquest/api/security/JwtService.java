package com.eduquest.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtService {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-access}") long accessTokenExpiration,
                      @Value("${jwt.expiration-refresh}") long refreshTokenExpiration) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    private String buildToken(UserPrincipal principal, String type, long expirationMillis) {
        Date now = new Date();
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .subject(principal.getUsername())
                .claim(CLAIM_USER_ID, principal.getId())
                .claim(CLAIM_EMAIL, principal.getEmail())
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(signingKey)
                .compact();
    }

    public String generateAccessToken(UserPrincipal principal) {
        return buildToken(principal, TYPE_ACCESS, accessTokenExpiration);
    }

    public String generateRefreshToken(UserPrincipal principal) {
        return buildToken(principal, TYPE_REFRESH, refreshTokenExpiration);
    }

    public boolean isValidToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public Long getUserId(String token) {
        Object value = extractClaims(token).get(CLAIM_USER_ID);
        return value instanceof Number number ? number.longValue() : null;
    }

    public String getEmail(String token) {
        return extractClaims(token).get(CLAIM_EMAIL, String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        return extractClaims(token).get(CLAIM_ROLES, List.class);
    }

    public String getTokenType(String token) {
        return extractClaims(token).get(CLAIM_TYPE, String.class);
    }
}