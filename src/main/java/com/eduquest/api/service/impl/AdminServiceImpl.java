package com.eduquest.api.service.impl;

import com.eduquest.api.dto.response.AdminStatsDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.entity.ReportStatus;
import com.eduquest.api.entity.Role;
import com.eduquest.api.entity.RoleName;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.DocumentRepository;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.ReportRepository;
import com.eduquest.api.repository.RoleRepository;
import com.eduquest.api.repository.StudySessionRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.AdminService;
import com.eduquest.api.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    static final int MANAGER_MAX_SUSPENSION_DAYS = 7;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final NotificationService notificationService;
    private final DocumentRepository documentRepository;
    private final PrivateGroupRepository groupRepository;
    private final StudySessionRepository sessionRepository;
    private final ReportRepository reportRepository;

    public AdminServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                            NotificationService notificationService, DocumentRepository documentRepository,
                            PrivateGroupRepository groupRepository, StudySessionRepository sessionRepository,
                            ReportRepository reportRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.notificationService = notificationService;
        this.documentRepository = documentRepository;
        this.groupRepository = groupRepository;
        this.sessionRepository = sessionRepository;
        this.reportRepository = reportRepository;
    }

    private UserResponseDTO mapToResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
        dto.setRoles(roles);
        dto.setAvatarUrl(UserResponseDTO.avatarUrlFor(user.getId(), user.getAvatarVersion()));
        dto.setAdvisor(user.isAdvisor());
        dto.setBanned(user.isBanned());
        dto.setBanReason(user.getBanReason());
        dto.setBannedAt(user.getBannedAt());
        dto.setBannedUntil(user.isBanned() ? user.getBannedUntil() : null);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<UserResponseDTO> getAllUsers(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);
        List<UserResponseDTO> content = userPage.getContent().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
        return PageResponseDTO.of(content, userPage.getNumber(), userPage.getSize(), userPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        return mapToResponseDTO(user);
    }

    @Override
    @Transactional
    public UserResponseDTO updateUserRole(Long id, String roleName) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        RoleName roleEnum;
        try {
            roleEnum = RoleName.valueOf(roleName);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Rol inválido: " + roleName);
        }

        Role role = roleRepository.findByName(roleEnum)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no existe en la base de datos: " + roleName));

        Set<Role> roles = user.getRoles();
        roles.clear();
        roles.add(role);

        return mapToResponseDTO(user);
    }

    @Override
    @Transactional
    public UserResponseDTO banUser(Long id, String reason, Integer days, String moderatorUsername) {
        User user = findUser(id);
        User moderator = findModerator(moderatorUsername);
        if (user.getId().equals(moderator.getId())) {
            throw new InvalidOperationException("No puedes suspender tu propia cuenta");
        }
        if (user.hasRole(RoleName.ROLE_ADMIN)) {
            throw new InvalidOperationException("No se puede vetar a otro administrador");
        }
        if (!moderator.hasRole(RoleName.ROLE_ADMIN)) {
            if (user.hasRole(RoleName.ROLE_MANAGER)) {
                throw new ForbiddenException("Solo un administrador puede suspender a un gestor");
            }
            if (days == null || days > MANAGER_MAX_SUSPENSION_DAYS) {
                throw new ForbiddenException("Como gestor puedes suspender hasta " + MANAGER_MAX_SUSPENSION_DAYS
                        + " días; el veto permanente es del administrador");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        user.setBanned(true);
        user.setBanReason(reason.trim());
        user.setBannedAt(now);
        user.setBannedUntil(days != null ? now.plusDays(days) : null);
        return mapToResponseDTO(userRepository.save(user));
    }

    @Override
    @Transactional
    public void warnUser(Long id, String message, String moderatorUsername) {
        User user = findUser(id);
        if (user.getUsername().equals(moderatorUsername)) {
            throw new InvalidOperationException("No puedes enviarte una advertencia a ti mismo");
        }
        notificationService.notifyWarning(user, message);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatsDTO getStats() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        return new AdminStatsDTO(
                userRepository.count(),
                userRepository.countByCreatedAtAfter(weekAgo),
                userRepository.countSuspended(),
                documentRepository.count(),
                documentRepository.countByUploadedAtAfter(weekAgo),
                groupRepository.count(),
                sessionRepository.countByCreatedAtAfter(weekAgo),
                reportRepository.countByStatus(ReportStatus.OPEN));
    }

    private User findModerator(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    @Override
    @Transactional
    public UserResponseDTO unbanUser(Long id) {
        User user = findUser(id);
        user.setBanned(false);
        user.setBanReason(null);
        user.setBannedAt(null);
        user.setBannedUntil(null);
        return mapToResponseDTO(userRepository.save(user));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }
}
