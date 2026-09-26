package com.eduquest.api.event;

import com.eduquest.api.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OnDocumentUploadedListener {

    private static final Logger log = LoggerFactory.getLogger(OnDocumentUploadedListener.class);

    private final EmailService emailService;

    public OnDocumentUploadedListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDocumentUploaded(OnDocumentUploadedEvent event) {
        log.info("Procesamiento asíncrono del documento {} (id={})", event.getDocumentTitle(), event.getDocumentId());
        emailService.sendDocumentUploadConfirmation(event.getUploaderEmail(), event.getDocumentTitle());
    }
}