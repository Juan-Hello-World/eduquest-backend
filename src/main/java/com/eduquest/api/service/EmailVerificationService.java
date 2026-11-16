package com.eduquest.api.service;

import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.entity.User;

public interface EmailVerificationService {
    boolean isEnabled();
    void sendCode(User user);
    void resend(String username);
    UserResponseDTO verify(String username, String code);
}
