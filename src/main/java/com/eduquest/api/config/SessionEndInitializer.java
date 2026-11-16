package com.eduquest.api.config;

import com.eduquest.api.entity.StudySession;
import com.eduquest.api.repository.StudySessionRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class SessionEndInitializer implements CommandLineRunner {

    private final StudySessionRepository sessionRepository;

    public SessionEndInitializer(StudySessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        List<StudySession> missing = sessionRepository.findByEndsAtIsNull();
        missing.forEach(session -> session.setEndsAt(session.getEffectiveEnd()));
        sessionRepository.saveAll(missing);
    }
}
