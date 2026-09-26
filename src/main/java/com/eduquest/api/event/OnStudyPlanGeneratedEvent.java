package com.eduquest.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OnStudyPlanGeneratedEvent extends ApplicationEvent {
    private final String username;
    private final String courseName;
    private final String difficulty;

    public OnStudyPlanGeneratedEvent(Object source, String username, String courseName, String difficulty) {
        super(source);
        this.username = username;
        this.courseName = courseName;
        this.difficulty = difficulty;
    }
}