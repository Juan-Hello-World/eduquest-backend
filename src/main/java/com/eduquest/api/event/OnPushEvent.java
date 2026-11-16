package com.eduquest.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class OnPushEvent extends ApplicationEvent {

    public record PushMessage(Long userId, String title, String body, String link) {
    }

    private final List<PushMessage> messages;

    public OnPushEvent(Object source, List<PushMessage> messages) {
        super(source);
        this.messages = List.copyOf(messages);
    }
}
