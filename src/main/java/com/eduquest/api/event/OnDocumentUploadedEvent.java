package com.eduquest.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OnDocumentUploadedEvent extends ApplicationEvent {
    private final Long documentId;
    private final String documentTitle;
    private final String uploaderEmail;

    public OnDocumentUploadedEvent(Object source, Long documentId, String documentTitle, String uploaderEmail) {
        super(source);
        this.documentId = documentId;
        this.documentTitle = documentTitle;
        this.uploaderEmail = uploaderEmail;
    }
}