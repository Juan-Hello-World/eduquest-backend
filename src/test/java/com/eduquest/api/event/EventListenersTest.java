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
    @Mock com.eduquest.api.service.NotificationService notificationService;
    @Mock com.eduquest.api.service.GamificationService gamificationService;

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

    @Test
    void passwordResetListener_sendsCodeByEmail() {
        PasswordResetListener listener = new PasswordResetListener(emailService);

        listener.handlePasswordResetRequested(new OnPasswordResetRequestedEvent(
                new Object(), "alice@utec.edu.pe", "alice", "123456", "https://eduquest.app/reset-password"));

        verify(emailService).sendPasswordResetCode("alice@utec.edu.pe", "alice", "123456", "https://eduquest.app/reset-password");
    }

    @Test
    void notificationListener_routesEachEvent() {
        NotificationListener listener = new NotificationListener(notificationService, gamificationService);

        listener.handleGroupActivity(new OnGroupActivityEvent(new Object(), 3L, 1L,
                com.eduquest.api.entity.NotificationType.GROUP_DOCUMENT, "Apuntes"));
        listener.handleDirectMessage(new OnDirectMessageSentEvent(new Object(), 1L, 2L, "Hola"));
        listener.handleUserActivity(new OnUserActivityEvent(new Object(), 1L));

        verify(notificationService).notifyGroupActivity(3L, 1L, com.eduquest.api.entity.NotificationType.GROUP_DOCUMENT, "Apuntes");
        verify(notificationService).notifyDirectMessage(1L, 2L, "Hola");
        verify(gamificationService).syncBadges(1L);
    }
}
