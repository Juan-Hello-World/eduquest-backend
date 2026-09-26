package com.eduquest.api.controller;

import com.eduquest.api.dto.request.LoginRequestDTO;
import com.eduquest.api.dto.request.RefreshTokenRequestDTO;
import com.eduquest.api.dto.request.RegisterRequestDTO;
import com.eduquest.api.dto.response.AuthResponseDTO;
import com.eduquest.api.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthControllerTest {

    @Mock AuthService authService;

    @InjectMocks AuthController authController;

    private AuthResponseDTO response() {
        return new AuthResponseDTO("jwt", "refresh", 1L, "alice", "alice@utec.edu.pe",
                Set.of("ROLE_USER"));
    }

    @Test
    void authenticateUser_returnsOk() {
        when(authService.authenticateUser(any())).thenReturn(response());

        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("alice");
        request.setPassword("password");

        ResponseEntity<AuthResponseDTO> result = authController.authenticateUser(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getUsername()).isEqualTo("alice");
    }

    @Test
    void registerUser_returnsCreated() {
        when(authService.registerUser(any())).thenReturn(response());

        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("alice");
        request.setEmail("alice@utec.edu.pe");
        request.setPassword("password1");

        ResponseEntity<AuthResponseDTO> result = authController.registerUser(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getToken()).isEqualTo("jwt");
    }

    @Test
    void refreshAccessToken_returnsOk() {
        when(authService.refreshToken(anyString())).thenReturn(response());

        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO();
        request.setRefreshToken("old-refresh");

        ResponseEntity<AuthResponseDTO> result = authController.refreshAccessToken(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getRefreshToken()).isEqualTo("refresh");
    }
}