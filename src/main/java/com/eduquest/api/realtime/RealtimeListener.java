package com.eduquest.api.realtime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RealtimeListener {

    private final ChatSocketHandler socketHandler;

    public RealtimeListener(ChatSocketHandler socketHandler) {
        this.socketHandler = socketHandler;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(RealtimeEvent event) {
        socketHandler.sendToUsers(event.getUserIds(), event.getPayload());
    }
}
