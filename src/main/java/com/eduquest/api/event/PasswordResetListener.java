package com.eduquest.api.event;

import com.eduquest.api.service.EmailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PasswordResetListener {

    private final EmailService emailService;

    public PasswordResetListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePasswordResetRequested(OnPasswordResetRequestedEvent event) {
        emailService.sendPasswordResetCode(event.getEmail(), event.getUsername(), event.getCode(), event.getResetLink());
    }
}
