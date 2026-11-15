package com.eduquest.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OnDirectMessageSentEvent extends ApplicationEvent {
    private final Long senderId;
    private final Long recipientId;
    private final String preview;

    public OnDirectMessageSentEvent(Object source, Long senderId, Long recipientId, String preview) {
        super(source);
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.preview = preview;
    }
}
