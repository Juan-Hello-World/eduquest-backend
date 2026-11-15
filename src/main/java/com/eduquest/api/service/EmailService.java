package com.eduquest.api.service;

public interface EmailService {
    void sendWelcomeEmail(String to, String username);
    void sendDocumentUploadConfirmation(String to, String documentTitle);
    void sendPasswordResetCode(String to, String username, String code, String resetLink);
}