package com.eduquest.api.service.impl;

import com.eduquest.api.entity.User;
import com.eduquest.api.event.OnPasswordResetRequestedEvent;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    static final int CODE_MINUTES = 15;
    static final int MAX_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public PasswordResetServiceImpl(UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmailIgnoreCase(normalize(email))
                .filter(user -> !user.isBanned())
                .ifPresent(user -> {
                    String code = String.format("%06d", random.nextInt(1_000_000));
                    user.setResetCodeHash(passwordEncoder.encode(code));
                    user.setResetCodeExpiresAt(LocalDateTime.now().plusMinutes(CODE_MINUTES));
                    user.setResetAttempts(0);
                    userRepository.save(user);

                    String link = UriComponentsBuilder.fromUriString(frontendUrl)
                            .path("/reset-password")
                            .queryParam("email", user.getEmail())
                            .queryParam("code", code)
                            .encode()
                            .toUriString();
                    eventPublisher.publishEvent(new OnPasswordResetRequestedEvent(
                            this, user.getEmail(), user.getUsername(), code, link));
                });
    }

    @Override
    @Transactional(noRollbackFor = BadRequestException.class)
    public void resetPassword(String email, String code, String newPassword) {
        User user = userRepository.findByEmailIgnoreCase(normalize(email))
                .filter(found -> found.getResetCodeHash() != null && found.getResetCodeExpiresAt() != null)
                .filter(found -> found.getResetCodeExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new BadRequestException("El código no es válido o ya venció. Pide uno nuevo"));

        int attempts = user.getResetAttempts() == null ? 0 : user.getResetAttempts();
        if (attempts >= MAX_ATTEMPTS) {
            throw new BadRequestException("Demasiados intentos. Pide un código nuevo");
        }
        if (!passwordEncoder.matches(code, user.getResetCodeHash())) {
            user.setResetAttempts(attempts + 1);
            userRepository.save(user);
            throw new BadRequestException("El código no es correcto");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetCodeHash(null);
        user.setResetCodeExpiresAt(null);
        user.setResetAttempts(null);
        userRepository.save(user);
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim();
    }
}
