package com.eduquest.api.event;

import org.springframework.context.ApplicationEvent;

public class OnUserRegistrationEvent extends ApplicationEvent {
    private final String email;
    private final String username;

    public OnUserRegistrationEvent(Object source, String email, String username) {
        super(source);
        this.email = email;
        this.username = username;
    }

    public String getEmail() { return email; }
    public String getUsername() { return username; }
}