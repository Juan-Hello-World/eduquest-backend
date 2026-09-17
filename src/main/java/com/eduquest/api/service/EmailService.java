package com.eduquest.api.service;

public interface EmailService {
    void sendWelcomeEmail(String to, String username);
}