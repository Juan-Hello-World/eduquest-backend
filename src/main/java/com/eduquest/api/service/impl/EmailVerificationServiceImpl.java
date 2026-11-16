package com.eduquest.api.service.impl;

import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.entity.User;
import com.eduquest.api.event.OnAccountEmailEvent;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.EmailVerificationService;
import com.eduquest.api.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    static final int CODE_MINUTES = 30;
    static final int MAX_ATTEMPTS = 5;
    static final int RESEND_SECONDS = 60;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final UserService userService;
    private final boolean enabled;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationServiceImpl(UserRepository userRepository,
                                        PasswordEncoder passwordEncoder,
                                        ApplicationEventPublisher eventPublisher,
                                        UserService userService,
                                        @Value("${app.email-verification.enabled:true}") boolean enabled) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.userService = userService;
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    @Transactional
    public void sendCode(User user) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now();
        user.setVerificationCodeHash(passwordEncoder.encode(code));
        user.setVerificationExpiresAt(now.plusMinutes(CODE_MINUTES));
        user.setVerificationSentAt(now);
        user.setVerificationAttempts(0);
        userRepository.save(user);
        eventPublisher.publishEvent(new OnAccountEmailEvent(this, OnAccountEmailEvent.Kind.VERIFICATION_CODE,
                user.getEmail(), user.getUsername(), code));
    }

    @Override
    @Transactional
    public void resend(String username) {
        User user = findUser(username);
        if (user.isEmailVerified()) {
            throw new BadRequestException("Tu correo ya está verificado");
        }
        LocalDateTime sentAt = user.getVerificationSentAt();
        if (sentAt != null && sentAt.plusSeconds(RESEND_SECONDS).isAfter(LocalDateTime.now())) {
            long wait = Math.max(1, Duration.between(LocalDateTime.now(), sentAt.plusSeconds(RESEND_SECONDS)).toSeconds());
            throw new BadRequestException("Espera " + wait + " segundos antes de pedir otro código");
        }
        sendCode(user);
    }

    @Override
    @Transactional(noRollbackFor = BadRequestException.class)
    public UserResponseDTO verify(String username, String code) {
        User user = findUser(username);
        if (user.isEmailVerified()) {
            return userService.getCurrentUser(username);
        }
        if (user.getVerificationCodeHash() == null || user.getVerificationExpiresAt() == null
                || user.getVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("El código venció. Pide uno nuevo");
        }
        int attempts = user.getVerificationAttempts() == null ? 0 : user.getVerificationAttempts();
        if (attempts >= MAX_ATTEMPTS) {
            throw new BadRequestException("Demasiados intentos. Pide un código nuevo");
        }
        if (code == null || !passwordEncoder.matches(code.trim(), user.getVerificationCodeHash())) {
            user.setVerificationAttempts(attempts + 1);
            userRepository.save(user);
            throw new BadRequestException("El código no es correcto");
        }
        user.setEmailVerified(true);
        user.setVerificationCodeHash(null);
        user.setVerificationExpiresAt(null);
        user.setVerificationAttempts(null);
        userRepository.save(user);
        return userService.getCurrentUser(username);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }
}
