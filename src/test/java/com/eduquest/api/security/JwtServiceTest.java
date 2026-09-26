package com.eduquest.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private JwtService jwtService;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60_000, 600_000);
        principal = new UserPrincipal(1L, "alice", "alice@utec.edu.pe", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void accessToken_roundTripsAllClaims() {
        String token = jwtService.generateAccessToken(principal);

        assertThat(jwtService.isValidToken(token)).isTrue();
        assertThat(jwtService.getTokenType(token)).isEqualTo("access");
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtService.getUserId(token)).isEqualTo(1L);
        assertThat(jwtService.getEmail(token)).isEqualTo("alice@utec.edu.pe");
        assertThat(jwtService.getRoles(token)).containsExactly("ROLE_USER");
    }

    @Test
    void refreshToken_isTypedAsRefresh() {
        String token = jwtService.generateRefreshToken(principal);

        assertThat(jwtService.isValidToken(token)).isTrue();
        assertThat(jwtService.getTokenType(token)).isEqualTo("refresh");
    }

    @Test
    void invalidToken_isRejected() {
        assertThat(jwtService.isValidToken("not-a-jwt")).isFalse();
        assertThat(jwtService.isValidToken("")).isFalse();
        assertThat(jwtService.isValidToken(null)).isFalse();
    }

    @Test
    void tokenSignedWithOtherKey_isRejected() {
        JwtService other = new JwtService("8642097531fedcba8642097531fedcba8642097531fedcba8642097531fedcba",
                60_000, 600_000);
        String token = other.generateAccessToken(principal);

        assertThat(jwtService.isValidToken(token)).isFalse();
    }
}