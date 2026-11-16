package com.eduquest.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OnPublicDocumentEvent extends ApplicationEvent {
    private final Long authorId;
    private final String documentTitle;

    public OnPublicDocumentEvent(Object source, Long authorId, String documentTitle) {
        super(source);
        this.authorId = authorId;
        this.documentTitle = documentTitle;
    }
}
