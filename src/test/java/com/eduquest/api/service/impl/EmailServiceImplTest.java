package com.eduquest.api.service.impl;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceImplTest {

    @Mock JavaMailSender mailSender;
    @Mock SpringTemplateEngine templateEngine;

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(mailSender, templateEngine);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html><body>Hola</body></html>");
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getDefaultInstance(new Properties())));
    }

    @Test
    void sendWelcomeEmail_rendersTemplateAndSendsMessage() {
        emailService.sendWelcomeEmail("alice@utec.edu.pe", "alice");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendDocumentUploadConfirmation_rendersTemplateAndSendsMessage() {
        emailService.sendDocumentUploadConfirmation("alice@utec.edu.pe", "Apuntes");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendPasswordResetCode_rendersTemplateAndSendsMessage() {
        emailService.sendPasswordResetCode("alice@utec.edu.pe", "alice", "123456", "https://eduquest.app/reset-password");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendVerificationAndAppealEmails_renderAndSend() {
        emailService.sendVerificationCode("alice@utec.edu.pe", "alice", "123456");
        emailService.sendAppealDecision("bob@utec.edu.pe", "bob", true, null);
        emailService.sendAppealDecision("bob@utec.edu.pe", "bob", false, "Se mantiene");

        verify(mailSender, org.mockito.Mockito.times(3)).send(any(MimeMessage.class));
    }
}
