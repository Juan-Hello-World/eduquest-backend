package com.eduquest.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OnUserActivityEvent extends ApplicationEvent {
    private final Long userId;

    public OnUserActivityEvent(Object source, Long userId) {
        super(source);
        this.userId = userId;
    }
}
