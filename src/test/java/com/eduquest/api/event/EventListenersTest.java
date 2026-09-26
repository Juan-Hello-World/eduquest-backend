package com.eduquest.api.event;

import com.eduquest.api.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventListenersTest {

    @Mock EmailService emailService;

    @Test
    void userRegistrationListener_sendsWelcomeEmail() {
        UserRegistrationListener listener = new UserRegistrationListener(emailService);

        listener.handleUserRegistration(new OnUserRegistrationEvent(new Object(), "alice@utec.edu.pe", "alice"));

        verify(emailService).sendWelcomeEmail("alice@utec.edu.pe", "alice");
    }

    @Test
    void documentUploadedListener_sendsConfirmationEmail() {
        OnDocumentUploadedListener listener = new OnDocumentUploadedListener(emailService);

        listener.handleDocumentUploaded(
                new OnDocumentUploadedEvent(new Object(), 1L, "Apuntes", "alice@utec.edu.pe"));

        verify(emailService).sendDocumentUploadConfirmation("alice@utec.edu.pe", "Apuntes");
    }

    @Test
    void studyPlanGeneratedListener_onlyLogs() {
        OnStudyPlanGeneratedListener listener = new OnStudyPlanGeneratedListener();

        listener.handleStudyPlanGenerated(
                new OnStudyPlanGeneratedEvent(new Object(), "alice", "Matematica Discreta", "Alta"));

        verify(emailService, org.mockito.Mockito.never())
                .sendWelcomeEmail(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}