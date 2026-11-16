package com.eduquest.api.service.impl;

import com.eduquest.api.entity.PushToken;
import com.eduquest.api.entity.User;
import com.eduquest.api.event.OnPushEvent;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.PushTokenRepository;
import com.eduquest.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExpoPushNotificationServiceTest {

    private static final String URL = "https://push.test/send";
    private final PushTokenRepository tokenRepository = mock(PushTokenRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private ExpoPushNotificationService service;
    private User alice;

    @BeforeEach
    void setUp() {
        service = new ExpoPushNotificationService(tokenRepository, userRepository, restTemplate, URL);
        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
    }

    private PushToken token(String value) {
        PushToken token = new PushToken();
        token.setToken(value);
        token.setUser(alice);
        return token;
    }

    @Test
    void register_upsertsTokenForUser() {
        when(tokenRepository.findByToken("ExponentPushToken[abc]")).thenReturn(Optional.empty());

        service.register("alice", "ExponentPushToken[abc]");

        ArgumentCaptor<PushToken> captor = ArgumentCaptor.forClass(PushToken.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(alice);
        assertThatThrownBy(() -> service.register("ghost", "x")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unregister_deletesOnlyOwnToken() {
        service.unregister("alice", "ExponentPushToken[abc]");
        service.unregister("ghost", "ExponentPushToken[abc]");

        verify(tokenRepository).deleteByTokenAndUserId("ExponentPushToken[abc]", 1L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void send_postsToExpoAndRemovesUnregisteredDevices() {
        when(tokenRepository.findByUserIdIn(Set.of(1L))).thenReturn(List.of(token("t-ok"), token("t-dead")));
        when(restTemplate.postForObject(eq(URL), any(HttpEntity.class), eq(Map.class))).thenReturn(Map.of("data", List.of(
                Map.of("status", "ok"),
                Map.of("status", "error", "details", Map.of("error", "DeviceNotRegistered")))));

        service.send(List.of(new OnPushEvent.PushMessage(1L, "Hola", "Mensaje", null)));

        ArgumentCaptor<HttpEntity<List<Map<String, Object>>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(eq(URL), captor.capture(), eq(Map.class));
        assertThat(captor.getValue().getBody()).hasSize(2);
        verify(tokenRepository).deleteByTokenIn(Set.of("t-dead"));
    }

    @Test
    void send_ignoresEmptyInputsAndNetworkErrors() {
        service.send(List.of());
        verifyNoInteractions(restTemplate);

        when(tokenRepository.findByUserIdIn(Set.of(2L))).thenReturn(List.of());
        service.send(List.of(new OnPushEvent.PushMessage(2L, "Hola", "x", "/a")));
        verifyNoInteractions(restTemplate);

        when(tokenRepository.findByUserIdIn(Set.of(1L))).thenReturn(List.of(token("t-ok")));
        when(restTemplate.postForObject(eq(URL), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new ResourceAccessException("sin red"));
        service.send(List.of(new OnPushEvent.PushMessage(1L, "Hola", "x", "/forum/1")));
        verify(tokenRepository, never()).deleteByTokenIn(any());
    }
}
