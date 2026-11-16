package com.eduquest.api.event;

import com.eduquest.api.service.GamificationService;
import com.eduquest.api.service.NotificationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationListener {

    private final NotificationService notificationService;
    private final GamificationService gamificationService;

    public NotificationListener(NotificationService notificationService, GamificationService gamificationService) {
        this.notificationService = notificationService;
        this.gamificationService = gamificationService;
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGroupActivity(OnGroupActivityEvent event) {
        notificationService.notifyGroupActivity(event.getGroupId(), event.getActorId(), event.getType(), event.getSubject());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDirectMessage(OnDirectMessageSentEvent event) {
        notificationService.notifyDirectMessage(event.getSenderId(), event.getRecipientId(), event.getPreview());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePublicDocument(OnPublicDocumentEvent event) {
        notificationService.notifyFollowers(event.getAuthorId(), event.getDocumentTitle());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserActivity(OnUserActivityEvent event) {
        gamificationService.syncBadges(event.getUserId());
    }
}
