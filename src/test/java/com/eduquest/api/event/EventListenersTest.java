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

    @Test
    void accountEmailListener_sendsTheRightEmail() {
        AccountEmailListener listener = new AccountEmailListener(emailService);

        listener.handle(new OnAccountEmailEvent(new Object(), OnAccountEmailEvent.Kind.VERIFICATION_CODE,
                "alice@utec.edu.pe", "alice", "123456"));
        listener.handle(new OnAccountEmailEvent(new Object(), OnAccountEmailEvent.Kind.APPEAL_ACCEPTED,
                "bob@utec.edu.pe", "bob", "Ok"));
        listener.handle(new OnAccountEmailEvent(new Object(), OnAccountEmailEvent.Kind.APPEAL_REJECTED,
                "bob@utec.edu.pe", "bob", null));

        verify(emailService).sendVerificationCode("alice@utec.edu.pe", "alice", "123456");
        verify(emailService).sendAppealDecision("bob@utec.edu.pe", "bob", true, "Ok");
        verify(emailService).sendAppealDecision("bob@utec.edu.pe", "bob", false, null);
    }

    @Test
    void notificationListener_notifiesFollowersOfPublicDocuments() {
        NotificationListener listener = new NotificationListener(notificationService, gamificationService);

        listener.handlePublicDocument(new OnPublicDocumentEvent(new Object(), 1L, "Apuntes"));

        verify(notificationService).notifyFollowers(1L, "Apuntes");
    }

    @Test
    void pushListener_sendsMessages() {
        com.eduquest.api.service.PushNotificationService pushService =
                org.mockito.Mockito.mock(com.eduquest.api.service.PushNotificationService.class);
        PushListener listener = new PushListener(pushService);
        OnPushEvent.PushMessage message = new OnPushEvent.PushMessage(1L, "Hola", "Cuerpo", "/forum/1");

        listener.handle(new OnPushEvent(new Object(), java.util.List.of(message)));

        verify(pushService).send(java.util.List.of(message));
    }
}
