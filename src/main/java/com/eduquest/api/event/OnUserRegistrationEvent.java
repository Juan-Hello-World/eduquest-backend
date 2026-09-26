package com.eduquest.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OnUserRegistrationEvent extends ApplicationEvent {
    private final String email;
    private final String username;

    public OnUserRegistrationEvent(Object source, String email, String username) {
        super(source);
        this.email = email;
        this.username = username;
    }
}