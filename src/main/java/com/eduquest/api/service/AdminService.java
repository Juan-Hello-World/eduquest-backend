package com.eduquest.api.service;

import com.eduquest.api.dto.response.AdminStatsDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import org.springframework.data.domain.Pageable;

public interface AdminService {
    PageResponseDTO<UserResponseDTO> getAllUsers(Pageable pageable);
    UserResponseDTO getUserById(Long id);
    UserResponseDTO updateUserRole(Long id, String roleName);
    UserResponseDTO banUser(Long id, String reason, Integer days, String moderatorUsername);
    void warnUser(Long id, String message, String moderatorUsername);
    AdminStatsDTO getStats();
    UserResponseDTO unbanUser(Long id);
}