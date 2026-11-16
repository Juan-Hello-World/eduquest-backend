package com.eduquest.api.controller;

import com.eduquest.api.dto.request.PushTokenRequestDTO;
import com.eduquest.api.service.PushNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PushTokenControllerTest {

    @Test
    void registerAndUnregister_delegateToService() {
        PushNotificationService service = mock(PushNotificationService.class);
        PushTokenController controller = new PushTokenController(service);
        PushTokenRequestDTO request = new PushTokenRequestDTO();
        request.setToken("ExponentPushToken[abcdefghijk]");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("alice", null);

        assertThat(controller.register(request, auth).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.unregister(request, auth).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).register("alice", "ExponentPushToken[abcdefghijk]");
        verify(service).unregister("alice", "ExponentPushToken[abcdefghijk]");
    }
}
