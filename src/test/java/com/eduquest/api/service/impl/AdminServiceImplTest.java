package com.eduquest.api.service.impl;

import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.entity.Role;
import com.eduquest.api.entity.RoleName;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.RoleRepository;
import com.eduquest.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;

    @InjectMocks AdminServiceImpl adminService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@utec.edu.pe");
        user.setRoles(new java.util.HashSet<>(Set.of(new Role(1L, RoleName.ROLE_USER))));
    }

    @Test
    void getAllUsers_returnsPaginatedUsersWithRoles() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user), pageable, 1));

        PageResponseDTO<UserResponseDTO> page = adminService.getAllUsers(pageable);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).getUsername()).isEqualTo("alice");
        assertThat(page.content().get(0).getRoles()).contains("ROLE_USER");
    }

    @Test
    void getUserById_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponseDTO response = adminService.getUserById(1L);

        assertThat(response.getEmail()).isEqualTo("alice@utec.edu.pe");
    }

    @Test
    void getUserById_withUnknownId_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateUserRole_withValidRole_updatesUser() {
        Role manager = new Role(3L, RoleName.ROLE_MANAGER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.ROLE_MANAGER)).thenReturn(Optional.of(manager));

        UserResponseDTO response = adminService.updateUserRole(1L, "ROLE_MANAGER");

        assertThat(response.getRoles()).containsExactly("ROLE_MANAGER");
    }

    @Test
    void updateUserRole_withInvalidRoleName_throwsBadRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adminService.updateUserRole(1L, "ROLE_HACKER"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateUserRole_withUnknownUser_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateUserRole(99L, "ROLE_MANAGER"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}