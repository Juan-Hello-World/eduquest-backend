package com.eduquest.api.service;

import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import org.springframework.data.domain.Pageable;

public interface AdminService {
    PageResponseDTO<UserResponseDTO> getAllUsers(Pageable pageable);
    UserResponseDTO getUserById(Long id);
    UserResponseDTO updateUserRole(Long id, String roleName);
}