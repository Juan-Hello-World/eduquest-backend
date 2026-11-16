package com.eduquest.api.event;

import com.eduquest.api.service.EmailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AccountEmailListener {

    private final EmailService emailService;

    public AccountEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OnAccountEmailEvent event) {
        switch (event.getKind()) {
            case VERIFICATION_CODE -> emailService.sendVerificationCode(event.getEmail(), event.getUsername(), event.getDetail());
            case APPEAL_ACCEPTED -> emailService.sendAppealDecision(event.getEmail(), event.getUsername(), true, event.getDetail());
            case APPEAL_REJECTED -> emailService.sendAppealDecision(event.getEmail(), event.getUsername(), false, event.getDetail());
        }
    }
}
