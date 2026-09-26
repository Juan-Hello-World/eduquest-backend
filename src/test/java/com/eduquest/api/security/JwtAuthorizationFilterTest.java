package com.eduquest.api.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.servlet.FilterChain;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthorizationFilterTest {

    @Mock JwtService jwtService;
    @Mock UserDetailsServiceImpl userDetailsService;

    private JwtAuthorizationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthorizationFilter(jwtService, userDetailsService);
    }

    @AfterEach
    void cleanContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/v1/users/me");
        return req;
    }

    @Test
    void withValidAccessToken_setsAuthenticationInContext() throws Exception {
        UserDetails details = mock(UserDetails.class);
        when(details.getAuthorities()).thenReturn(List.of());
        when(jwtService.isValidToken("tok")).thenReturn(true);
        when(jwtService.getTokenType("tok")).thenReturn("access");
        when(jwtService.extractUsername("tok")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(details);

        MockHttpServletRequest request = request();
        request.addHeader("Authorization", "Bearer tok");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void withInvalidToken_doesNotSetAuthentication() throws Exception {
        when(jwtService.isValidToken("bad")).thenReturn(false);

        MockHttpServletRequest request = request();
        request.addHeader("Authorization", "Bearer bad");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void withoutAuthorizationHeader_doesNotInteractWithJwt() throws Exception {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).extractUsername(org.mockito.ArgumentMatchers.anyString());
        verify(chain).doFilter(request, response);
    }
}