package com.eduquest.api.service;

public interface PasswordResetService {
    void requestReset(String email);
    void resetPassword(String email, String code, String newPassword);
}
