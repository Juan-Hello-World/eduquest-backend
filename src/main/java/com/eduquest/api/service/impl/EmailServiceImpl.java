package com.eduquest.api.service.impl;

import com.eduquest.api.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendWelcomeEmail(String to, String username) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("¡Bienvenido a EduQuest, " + username + "!");

            String htmlMsg = "<h3>Hola " + username + ",</h3>"
                    + "<p>Gracias por registrarte en <b>EduQuest</b>. Tu cuenta ha sido creada exitosamente.</p>"
                    + "<p>Ya puedes empezar a explorar el repositorio público y unirte a grupos privados.</p>";

            helper.setText(htmlMsg, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            System.err.println("Error enviando el correo de bienvenida: " + e.getMessage());
        }
    }
}