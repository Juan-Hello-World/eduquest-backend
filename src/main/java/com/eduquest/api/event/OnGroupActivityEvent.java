package com.eduquest.api.event;

import com.eduquest.api.entity.NotificationType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OnGroupActivityEvent extends ApplicationEvent {
    private final Long groupId;
    private final Long actorId;
    private final NotificationType type;
    private final String subject;

    public OnGroupActivityEvent(Object source, Long groupId, Long actorId, NotificationType type, String subject) {
        super(source);
        this.groupId = groupId;
        this.actorId = actorId;
        this.type = type;
        this.subject = subject;
    }
}
