package com.eduquest.api.service.impl;

import com.eduquest.api.entity.User;
import com.eduquest.api.event.OnPasswordResetRequestedEvent;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordResetServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks PasswordResetServiceImpl passwordResetService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "frontendUrl", "https://eduquest.app");
        user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("Alice@utec.edu.pe");
        user.setPassword("hash");
        when(userRepository.findByEmailIgnoreCase("alice@utec.edu.pe")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "enc:" + inv.getArgument(0));
    }

    @Test
    void requestReset_storesHashedCodeAndPublishesEmailEvent() {
        passwordResetService.requestReset(" alice@utec.edu.pe ");

        ArgumentCaptor<OnPasswordResetRequestedEvent> captor = ArgumentCaptor.forClass(OnPasswordResetRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        OnPasswordResetRequestedEvent event = captor.getValue();
        assertThat(event.getCode()).matches("\\d{6}");
        assertThat(event.getResetLink()).startsWith("https://eduquest.app/reset-password?email=").contains("code=" + event.getCode());
        assertThat(user.getResetCodeHash()).isEqualTo("enc:" + event.getCode());
        assertThat(user.getResetCodeExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(14));
        assertThat(user.getResetAttempts()).isZero();
    }

    @Test
    void requestReset_unknownOrBannedEmail_doesNothing() {
        when(userRepository.findByEmailIgnoreCase("nadie@utec.edu.pe")).thenReturn(Optional.empty());
        user.setBanned(true);

        passwordResetService.requestReset("nadie@utec.edu.pe");
        passwordResetService.requestReset("alice@utec.edu.pe");
        passwordResetService.requestReset(null);

        verify(eventPublisher, never()).publishEvent(any());
    }

    private void withCode(int attempts, LocalDateTime expiresAt) {
        user.setResetCodeHash("code-hash");
        user.setResetCodeExpiresAt(expiresAt);
        user.setResetAttempts(attempts);
    }

    @Test
    void resetPassword_withValidCode_updatesPasswordAndClearsCode() {
        withCode(1, LocalDateTime.now().plusMinutes(5));
        when(passwordEncoder.matches("123456", "code-hash")).thenReturn(true);

        passwordResetService.resetPassword("alice@utec.edu.pe", "123456", "nueva123");

        assertThat(user.getPassword()).isEqualTo("enc:nueva123");
        assertThat(user.getResetCodeHash()).isNull();
        assertThat(user.getResetAttempts()).isNull();
    }

    @Test
    void resetPassword_withWrongCode_countsAttempt() {
        withCode(0, LocalDateTime.now().plusMinutes(5));
        when(passwordEncoder.matches("000000", "code-hash")).thenReturn(false);

        assertThatThrownBy(() -> passwordResetService.resetPassword("alice@utec.edu.pe", "000000", "nueva123"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no es correcto");
        assertThat(user.getResetAttempts()).isEqualTo(1);
    }

    @Test
    void resetPassword_afterTooManyAttemptsOrExpiry_fails() {
        withCode(5, LocalDateTime.now().plusMinutes(5));
        assertThatThrownBy(() -> passwordResetService.resetPassword("alice@utec.edu.pe", "123456", "nueva123"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Demasiados");

        withCode(0, LocalDateTime.now().minusMinutes(1));
        assertThatThrownBy(() -> passwordResetService.resetPassword("alice@utec.edu.pe", "123456", "nueva123"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("venció");

        user.setResetCodeHash(null);
        assertThatThrownBy(() -> passwordResetService.resetPassword("alice@utec.edu.pe", "123456", "nueva123"))
                .isInstanceOf(BadRequestException.class);
    }
}
