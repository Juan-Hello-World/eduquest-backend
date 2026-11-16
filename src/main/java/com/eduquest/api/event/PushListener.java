package com.eduquest.api.event;

import com.eduquest.api.service.PushNotificationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PushListener {

    private final PushNotificationService pushService;

    public PushListener(PushNotificationService pushService) {
        this.pushService = pushService;
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(OnPushEvent event) {
        pushService.send(event.getMessages());
    }
}
