package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.UpdateProfileRequestDTO;
import com.eduquest.api.dto.response.AuthResponseDTO;
import com.eduquest.api.dto.response.AvatarImageDTO;
import com.eduquest.api.dto.response.BadgeDTO;
import com.eduquest.api.dto.response.DocumentResponseDTO;
import com.eduquest.api.dto.response.GamificationProfileDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.dto.response.PublicProfileDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.dto.response.UserSummaryDTO;
import com.eduquest.api.entity.User;
import com.eduquest.api.entity.UserAvatar;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.DuplicateResourceException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.UserAvatarRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.security.JwtService;
import com.eduquest.api.security.UserPrincipal;
import com.eduquest.api.service.DocumentService;
import com.eduquest.api.service.GamificationService;
import com.eduquest.api.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final UserRepository userRepository;
    private final UserAvatarRepository userAvatarRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final GamificationService gamificationService;
    private final DocumentService documentService;

    public UserServiceImpl(UserRepository userRepository,
                           UserAvatarRepository userAvatarRepository,
                           JwtService jwtService,
                           PasswordEncoder passwordEncoder,
                           GamificationService gamificationService,
                           DocumentService documentService) {
        this.userRepository = userRepository;
        this.userAvatarRepository = userAvatarRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.gamificationService = gamificationService;
        this.documentService = documentService;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicProfileDTO getPublicProfile(String viewerUsername, Long userId) {
        User viewer = findUser(viewerUsername);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        GamificationProfileDTO gamification = gamificationService.getProfile(userId);
        List<BadgeDTO> earned = gamification.badges().stream().filter(BadgeDTO::earned).toList();
        return new PublicProfileDTO(
                user.getId(),
                user.getUsername(),
                UserResponseDTO.avatarUrlFor(user.getId(), user.getAvatarVersion()),
                user.isAdvisor(),
                user.getCreatedAt(),
                gamification.points(),
                gamification.weeklyPoints(),
                gamification.rank(),
                gamification.stats(),
                earned,
                userRepository.countFollowers(userId),
                userRepository.countFollowing(userId),
                userRepository.existsByIdAndFollowingId(viewer.getId(), userId),
                viewer.getId().equals(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<DocumentResponseDTO> getPublicDocuments(Long userId, Pageable pageable) {
        return documentService.getPublicDocumentsByAuthor(userId, pageable);
    }

    @Override
    public void follow(String followerUsername, Long userId) {
        User follower = findUser(followerUsername);
        if (follower.getId().equals(userId)) {
            throw new BadRequestException("No puedes seguirte a ti mismo");
        }
        User followed = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (follower.getFollowing().add(followed)) {
            userRepository.save(follower);
        }
    }

    @Override
    public void unfollow(String followerUsername, Long userId) {
        User follower = findUser(followerUsername);
        if (follower.getFollowing().removeIf(followed -> followed.getId().equals(userId))) {
            userRepository.save(follower);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryDTO> getFollowing(String username) {
        return userRepository.findFollowing(findUser(username).getId()).stream()
                .map(user -> new UserSummaryDTO(user.getId(), user.getUsername(),
                        UserResponseDTO.avatarUrlFor(user.getId(), user.getAvatarVersion()), user.isAdvisor()))
                .toList();
    }

    @Override
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = findUser(username);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("La contraseña actual no es correcta");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BadRequestException("La nueva contraseña debe ser distinta a la actual");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getCurrentUser(String username) {
        return toResponse(findUser(username));
    }

    @Override
    public AuthResponseDTO updateProfile(String username, UpdateProfileRequestDTO request) {
        User user = findUser(username);
        String newUsername = request.getUsername().trim();
        boolean changed = false;

        if (!newUsername.equals(user.getUsername())) {
            if (userRepository.existsByUsername(newUsername)) {
                throw new DuplicateResourceException("El nombre de usuario ya está en uso");
            }
            user.setUsername(newUsername);
            changed = true;
        }
        if (request.getAdvisor() != null && request.getAdvisor() != user.isAdvisor()) {
            user.setAdvisor(request.getAdvisor());
            changed = true;
        }
        if (changed) {
            user = userRepository.save(user);
        }

        UserPrincipal principal = UserPrincipal.build(user);
        AuthResponseDTO response = new AuthResponseDTO(
                jwtService.generateAccessToken(principal),
                jwtService.generateRefreshToken(principal),
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roleNames(user));
        response.setAvatarUrl(UserResponseDTO.avatarUrlFor(user.getId(), user.getAvatarVersion()));
        return response;
    }

    @Override
    public UserResponseDTO updateAvatar(String username, MultipartFile file) {
        String contentType = validateImage(file);
        User user = findUser(username);

        UserAvatar avatar = userAvatarRepository.findById(user.getId()).orElseGet(() -> {
            UserAvatar created = new UserAvatar();
            created.setUser(user);
            return created;
        });
        avatar.setData(readBytes(file));
        avatar.setContentType(contentType);
        avatar.setUpdatedAt(LocalDateTime.now());
        userAvatarRepository.save(avatar);

        user.setAvatarVersion(System.currentTimeMillis());
        return toResponse(userRepository.save(user));
    }

    @Override
    public void deleteAvatar(String username) {
        User user = findUser(username);
        if (userAvatarRepository.existsById(user.getId())) {
            userAvatarRepository.deleteById(user.getId());
        }
        user.setAvatarVersion(null);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AvatarImageDTO getAvatar(Long userId) {
        UserAvatar avatar = userAvatarRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Este usuario no tiene foto de perfil"));
        return new AvatarImageDTO(avatar.getData(), avatar.getContentType());
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private String validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Selecciona una imagen");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new BadRequestException("La imagen no debe superar los 2 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new BadRequestException("Formato no permitido. Usa una imagen JPG, PNG o WEBP");
        }
        return contentType;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("No se pudo leer la imagen");
        }
    }

    private Set<String> roleNames(User user) {
        return user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
    }

    private UserResponseDTO toResponse(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRoles(roleNames(user));
        dto.setAvatarUrl(UserResponseDTO.avatarUrlFor(user.getId(), user.getAvatarVersion()));
        dto.setAdvisor(user.isAdvisor());
        dto.setEmailVerified(user.isEmailVerified());
        dto.setAvailability(user.getAvailability() == null ? null : user.getAvailability().name());
        dto.setStatusText(user.getStatusText());
        dto.setStatusEmoji(user.getStatusEmoji());
        return dto;
    }
}
