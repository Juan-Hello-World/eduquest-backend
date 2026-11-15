package com.eduquest.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OnPasswordResetRequestedEvent extends ApplicationEvent {
    private final String email;
    private final String username;
    private final String code;
    private final String resetLink;

    public OnPasswordResetRequestedEvent(Object source, String email, String username, String code, String resetLink) {
        super(source);
        this.email = email;
        this.username = username;
        this.code = code;
        this.resetLink = resetLink;
    }
}
