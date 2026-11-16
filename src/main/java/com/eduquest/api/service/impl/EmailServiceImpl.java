package com.eduquest.api.service.impl;

import com.eduquest.api.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    public EmailServiceImpl(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendWelcomeEmail(String to, String username) {
        sendHtmlEmail(to, "Bienvenido a EduQuest, " + username + "!",
                "email/welcome", Map.of("username", username));
    }

    @Override
    public void sendDocumentUploadConfirmation(String to, String documentTitle) {
        sendHtmlEmail(to, "EduQuest: documento publicado correctamente",
                "email/document-uploaded", Map.of("documentTitle", documentTitle));
    }

    @Override
    public void sendPasswordResetCode(String to, String username, String code, String resetLink) {
        sendHtmlEmail(to, "EduQuest: tu código para cambiar la contraseña",
                "email/password-reset", Map.of("username", username, "code", code, "resetLink", resetLink));
    }

    @Override
    public void sendVerificationCode(String to, String username, String code) {
        sendHtmlEmail(to, "EduQuest: confirma tu correo",
                "email/verify-email", Map.of("username", username, "code", code));
    }

    @Override
    public void sendAppealDecision(String to, String username, boolean accepted, String response) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("accepted", accepted);
        variables.put("response", response);
        sendHtmlEmail(to, accepted ? "EduQuest: levantamos tu suspensión" : "EduQuest: revisamos tu apelación",
                "email/appeal-decision", variables);
    }

    private String renderTemplate(String template, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(template, context);
    }

    private void sendHtmlEmail(String to, String subject, String template, Map<String, Object> variables) {
        try {
            String html = renderTemplate(template, variables);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (MessagingException ex) {
            log.error("Error enviando el correo a {}: {}", to, ex.getMessage());
        }
    }
}