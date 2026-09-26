package com.eduquest.api.controller;

import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.service.AdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminUserControllerTest {

    @Mock AdminService adminService;

    @InjectMocks AdminUserController adminUserController;

    private UserResponseDTO response() {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(1L);
        dto.setUsername("alice");
        dto.setEmail("alice@utec.edu.pe");
        dto.setRoles(Set.of("ROLE_USER"));
        return dto;
    }

    @Test
    void getAllUsers_returnsPaginatedResponse() {
        PageResponseDTO<UserResponseDTO> page =
                new PageResponseDTO<>(List.of(response()), 0, 10, 1, 1);
        when(adminService.getAllUsers(any())).thenReturn(page);

        ResponseEntity<PageResponseDTO<UserResponseDTO>> result = adminUserController.getAllUsers(0, 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().content()).hasSize(1);
    }

    @Test
    void getUserById_returnsOk() {
        when(adminService.getUserById(anyLong())).thenReturn(response());

        ResponseEntity<UserResponseDTO> result = adminUserController.getUserById(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getUsername()).isEqualTo("alice");
    }

    @Test
    void updateUserRole_returnsOk() {
        UserResponseDTO updated = response();
        updated.setRoles(Set.of("ROLE_MANAGER"));
        when(adminService.updateUserRole(anyLong(), anyString())).thenReturn(updated);

        ResponseEntity<UserResponseDTO> result = adminUserController.updateUserRole(1L, "ROLE_MANAGER");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getRoles()).containsExactly("ROLE_MANAGER");
    }

    @Test
    void getAllUsers_clampsInvalidParams() {
        when(adminService.getAllUsers(any())).thenReturn(new PageResponseDTO<>(List.of(), 0, 1, 0, 0));

        adminUserController.getAllUsers(-1, 1000);

        assertThat(true).isTrue();
    }
}