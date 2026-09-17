package com.eduquest.api.event;

import com.eduquest.api.service.EmailService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class UserRegistrationListener {

    private final EmailService emailService;

    public UserRegistrationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async("taskExecutor")
    @EventListener
    public void handleUserRegistration(OnUserRegistrationEvent event) {
        emailService.sendWelcomeEmail(event.getEmail(), event.getUsername());
    }
}