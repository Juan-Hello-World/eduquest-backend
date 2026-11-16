package com.eduquest.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OnAccountEmailEvent extends ApplicationEvent {

    public enum Kind { VERIFICATION_CODE, APPEAL_ACCEPTED, APPEAL_REJECTED }

    private final Kind kind;
    private final String email;
    private final String username;
    private final String detail;

    public OnAccountEmailEvent(Object source, Kind kind, String email, String username, String detail) {
        super(source);
        this.kind = kind;
        this.email = email;
        this.username = username;
        this.detail = detail;
    }
}
