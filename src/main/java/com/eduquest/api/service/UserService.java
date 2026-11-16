package com.eduquest.api.service;

import com.eduquest.api.dto.request.UpdateProfileRequestDTO;
import com.eduquest.api.dto.response.AuthResponseDTO;
import com.eduquest.api.dto.response.AvatarImageDTO;
import com.eduquest.api.dto.response.DocumentResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.dto.response.PublicProfileDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.dto.response.UserSummaryDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    UserResponseDTO getCurrentUser(String username);
    AuthResponseDTO updateProfile(String username, UpdateProfileRequestDTO request);
    UserResponseDTO updateAvatar(String username, MultipartFile file);
    void deleteAvatar(String username);
    void changePassword(String username, String currentPassword, String newPassword);
    AvatarImageDTO getAvatar(Long userId);
    PublicProfileDTO getPublicProfile(String viewerUsername, Long userId);
    PageResponseDTO<DocumentResponseDTO> getPublicDocuments(Long userId, Pageable pageable);
    void follow(String followerUsername, Long userId);
    void unfollow(String followerUsername, Long userId);
    List<UserSummaryDTO> getFollowing(String username);
}
