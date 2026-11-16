package com.eduquest.api.service.impl;

import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.entity.User;
import com.eduquest.api.event.OnAccountEmailEvent;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailVerificationServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final UserService userService = mock(UserService.class);
    private EmailVerificationServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationServiceImpl(userRepository, passwordEncoder, eventPublisher, userService, true);
        user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@utec.edu.pe");
        user.setEmailVerified(false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("code-hash");
        when(userService.getCurrentUser("alice")).thenReturn(new UserResponseDTO());
    }

    @Test
    void sendCode_storesHashAndPublishesEmail() {
        assertThat(service.isEnabled()).isTrue();

        service.sendCode(user);

        assertThat(user.getVerificationCodeHash()).isEqualTo("code-hash");
        assertThat(user.getVerificationExpiresAt()).isAfter(LocalDateTime.now());
        ArgumentCaptor<OnAccountEmailEvent> captor = ArgumentCaptor.forClass(OnAccountEmailEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getKind()).isEqualTo(OnAccountEmailEvent.Kind.VERIFICATION_CODE);
        assertThat(captor.getValue().getDetail()).matches("\\d{6}");
    }

    @Test
    void resend_respectsCooldown_andRejectsVerifiedAccounts() {
        user.setVerificationSentAt(LocalDateTime.now());
        assertThatThrownBy(() -> service.resend("alice")).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("segundos");

        user.setVerificationSentAt(LocalDateTime.now().minusMinutes(2));
        service.resend("alice");
        assertThat(user.getVerificationAttempts()).isZero();

        user.setEmailVerified(true);
        assertThatThrownBy(() -> service.resend("alice")).hasMessageContaining("ya está verificado");
    }

    @Test
    void verify_correctCode_marksVerified() {
        user.setVerificationCodeHash("code-hash");
        user.setVerificationExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(passwordEncoder.matches("123456", "code-hash")).thenReturn(true);

        service.verify("alice", " 123456 ");

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getVerificationCodeHash()).isNull();
        service.verify("alice", "000000");
        verify(userRepository, org.mockito.Mockito.times(1)).save(any(User.class));
    }

    @Test
    void verify_wrongExpiredOrTooManyAttempts_throws() {
        assertThatThrownBy(() -> service.verify("alice", "123456")).hasMessageContaining("venció");

        user.setVerificationCodeHash("code-hash");
        user.setVerificationExpiresAt(LocalDateTime.now().plusMinutes(5));
        assertThatThrownBy(() -> service.verify("alice", "999999")).hasMessageContaining("no es correcto");
        assertThat(user.getVerificationAttempts()).isEqualTo(1);
        assertThatThrownBy(() -> service.verify("alice", null)).hasMessageContaining("no es correcto");

        user.setVerificationAttempts(5);
        assertThatThrownBy(() -> service.verify("alice", "123456")).hasMessageContaining("Demasiados");
        assertThatThrownBy(() -> service.verify("ghost", "123456"))
                .isInstanceOf(com.eduquest.api.exception.ResourceNotFoundException.class);
    }
}
