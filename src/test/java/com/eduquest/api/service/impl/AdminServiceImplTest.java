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
import com.eduquest.api.exception.InvalidOperationException;
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
    @Mock com.eduquest.api.service.NotificationService notificationService;
    @Mock com.eduquest.api.repository.DocumentRepository documentRepository;
    @Mock com.eduquest.api.repository.PrivateGroupRepository groupRepository;
    @Mock com.eduquest.api.repository.StudySessionRepository sessionRepository;
    @Mock com.eduquest.api.repository.ReportRepository reportRepository;

    @InjectMocks AdminServiceImpl adminService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@utec.edu.pe");
        user.setRoles(new java.util.HashSet<>(Set.of(new Role(1L, RoleName.ROLE_USER))));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(moderator(9L, "admin", RoleName.ROLE_ADMIN)));
        when(userRepository.findByUsername("gestor")).thenReturn(Optional.of(moderator(8L, "gestor", RoleName.ROLE_MANAGER)));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
    }

    private static User moderator(Long id, String username, RoleName role) {
        User moderator = new User();
        moderator.setId(id);
        moderator.setUsername(username);
        moderator.setRoles(new java.util.HashSet<>(Set.of(new Role(id, role))));
        return moderator;
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

    @Test
    void banUser_storesReasonAndDate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponseDTO dto = adminService.banUser(1L, "  Publicó material ofensivo  ", null, "admin");

        assertThat(dto.isBanned()).isTrue();
        assertThat(dto.getBanReason()).isEqualTo("Publicó material ofensivo");
        assertThat(dto.getBannedAt()).isNotNull();
    }

    @Test
    void banUser_ownAccount_throwsInvalidOperation() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adminService.banUser(1L, "Motivo de prueba", null, "alice"))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void banUser_anotherAdmin_throwsInvalidOperation() {
        user.setRoles(new java.util.HashSet<>(Set.of(new Role(3L, RoleName.ROLE_ADMIN))));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adminService.banUser(1L, "Motivo de prueba", null, "admin"))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void unbanUser_clearsBan() {
        user.setBanned(true);
        user.setBanReason("Spam");
        user.setBannedAt(java.time.LocalDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponseDTO dto = adminService.unbanUser(1L);

        assertThat(dto.isBanned()).isFalse();
        assertThat(dto.getBanReason()).isNull();
        assertThat(dto.getBannedAt()).isNull();
    }

    @Test
    void banUser_managerCanOnlySuspendStudentsTemporarily() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        assertThatThrownBy(() -> adminService.banUser(1L, "Motivo de prueba", null, "gestor"))
                .isInstanceOf(com.eduquest.api.exception.ForbiddenException.class);
        assertThatThrownBy(() -> adminService.banUser(1L, "Motivo de prueba", 10, "gestor"))
                .isInstanceOf(com.eduquest.api.exception.ForbiddenException.class);

        UserResponseDTO dto = adminService.banUser(1L, "Motivo de prueba", 3, "gestor");
        assertThat(dto.isBanned()).isTrue();
        assertThat(dto.getBannedUntil()).isAfter(java.time.LocalDateTime.now().plusDays(2));

        user.setRoles(new java.util.HashSet<>(Set.of(new Role(2L, RoleName.ROLE_MANAGER))));
        assertThatThrownBy(() -> adminService.banUser(1L, "Motivo de prueba", 1, "gestor"))
                .isInstanceOf(com.eduquest.api.exception.ForbiddenException.class);
    }

    @Test
    void suspension_expiresByItself() {
        user.setBanned(true);
        user.setBannedUntil(java.time.LocalDateTime.now().minusMinutes(1));
        assertThat(user.isBanned()).isFalse();
        user.setBannedUntil(java.time.LocalDateTime.now().plusDays(1));
        assertThat(user.isBanned()).isTrue();
    }

    @Test
    void warnUser_sendsNotification() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        adminService.warnUser(1L, "Evita publicar spam en los grupos", "gestor");

        org.mockito.Mockito.verify(notificationService).notifyWarning(user, "Evita publicar spam en los grupos");
        assertThatThrownBy(() -> adminService.warnUser(1L, "Mensaje de prueba", "alice"))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void getStats_collectsCounters() {
        when(userRepository.count()).thenReturn(13L);
        when(userRepository.countByCreatedAtAfter(org.mockito.ArgumentMatchers.any())).thenReturn(2L);
        when(userRepository.countSuspended()).thenReturn(1L);
        when(documentRepository.count()).thenReturn(9L);
        when(documentRepository.countByUploadedAtAfter(org.mockito.ArgumentMatchers.any())).thenReturn(3L);
        when(groupRepository.count()).thenReturn(7L);
        when(sessionRepository.countByCreatedAtAfter(org.mockito.ArgumentMatchers.any())).thenReturn(4L);
        when(reportRepository.countByStatus(com.eduquest.api.entity.ReportStatus.OPEN)).thenReturn(5L);

        com.eduquest.api.dto.response.AdminStatsDTO stats = adminService.getStats();

        assertThat(stats.users()).isEqualTo(13);
        assertThat(stats.newUsersThisWeek()).isEqualTo(2);
        assertThat(stats.openReports()).isEqualTo(5);
        assertThat(stats.sessionsThisWeek()).isEqualTo(4);
    }
}
