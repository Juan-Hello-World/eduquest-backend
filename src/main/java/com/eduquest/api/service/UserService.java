package com.eduquest.api.service;

import com.eduquest.api.dto.request.UpdateProfileRequestDTO;
import com.eduquest.api.dto.response.AuthResponseDTO;
import com.eduquest.api.dto.response.AvatarImageDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    UserResponseDTO getCurrentUser(String username);
    AuthResponseDTO updateProfile(String username, UpdateProfileRequestDTO request);
    UserResponseDTO updateAvatar(String username, MultipartFile file);
    void deleteAvatar(String username);
    void changePassword(String username, String currentPassword, String newPassword);
    AvatarImageDTO getAvatar(Long userId);
}
