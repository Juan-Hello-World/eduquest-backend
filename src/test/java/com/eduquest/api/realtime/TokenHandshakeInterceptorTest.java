package com.eduquest.api.realtime;

import com.eduquest.api.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenHandshakeInterceptorTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final TokenHandshakeInterceptor interceptor = new TokenHandshakeInterceptor(jwtService);

    private ServerHttpRequest request(String uri) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(URI.create(uri));
        return request;
    }

    @Test
    void validAccessToken_storesUserInAttributes() {
        when(jwtService.isValidToken("good")).thenReturn(true);
        when(jwtService.getTokenType("good")).thenReturn("access");
        when(jwtService.getUserId("good")).thenReturn(7L);
        when(jwtService.extractUsername("good")).thenReturn("alice");
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = interceptor.beforeHandshake(request("ws://localhost:8080/ws?token=good"),
                mock(ServerHttpResponse.class), null, attributes);

        assertThat(accepted).isTrue();
        assertThat(attributes).containsEntry(ChatSocketHandler.USER_ID, 7L).containsEntry(ChatSocketHandler.USERNAME, "alice");
        interceptor.afterHandshake(null, null, null, null);
    }

    @Test
    void missingRefreshOrIncompleteToken_isRejected() {
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        assertThat(interceptor.beforeHandshake(request("ws://localhost:8080/ws"), response, null, new HashMap<>())).isFalse();

        when(jwtService.isValidToken("refresh")).thenReturn(true);
        when(jwtService.getTokenType("refresh")).thenReturn("refresh");
        assertThat(interceptor.beforeHandshake(request("ws://localhost:8080/ws?token=refresh"), response, null,
                new HashMap<>())).isFalse();

        when(jwtService.isValidToken("noid")).thenReturn(true);
        when(jwtService.getTokenType("noid")).thenReturn("access");
        when(jwtService.getUserId("noid")).thenReturn(null);
        assertThat(interceptor.beforeHandshake(request("ws://localhost:8080/ws?token=noid"), response, null,
                new HashMap<>())).isFalse();
        verify(response, org.mockito.Mockito.times(3)).setStatusCode(HttpStatus.UNAUTHORIZED);
    }
}
