package com.eduquest.api.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OnStudyPlanGeneratedListener {

    private static final Logger log = LoggerFactory.getLogger(OnStudyPlanGeneratedListener.class);

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudyPlanGenerated(OnStudyPlanGeneratedEvent event) {
        log.info("Plan de estudio generado para {} (curso: {}, dificultad: {})",
                event.getUsername(), event.getCourseName(), event.getDifficulty());
    }
}