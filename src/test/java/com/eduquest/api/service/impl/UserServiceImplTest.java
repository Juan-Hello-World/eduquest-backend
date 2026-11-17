package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.UpdateProfileRequestDTO;
import com.eduquest.api.dto.response.AuthResponseDTO;
import com.eduquest.api.dto.response.AvatarImageDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.entity.Role;
import com.eduquest.api.entity.RoleName;
import com.eduquest.api.entity.User;
import com.eduquest.api.entity.UserAvatar;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.DuplicateResourceException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.UserAvatarRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock UserAvatarRepository userAvatarRepository;
    @Mock org.springframework.beans.factory.ObjectProvider<com.eduquest.api.storage.ObjectStorage> objectStorage;
    @Mock com.eduquest.api.storage.ObjectStorage storage;
    @Mock JwtService jwtService;
    @Mock org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock com.eduquest.api.service.GamificationService gamificationService;
    @Mock com.eduquest.api.service.DocumentService documentService;

    @InjectMocks UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@utec.edu.pe");
        user.setPassword("hash");
        user.setRoles(new HashSet<>(Set.of(new Role(1L, RoleName.ROLE_USER))));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void getCurrentUser_withoutAvatar_returnsNullAvatarUrl() {
        UserResponseDTO dto = userService.getCurrentUser("alice");

        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getRoles()).containsExactly("ROLE_USER");
        assertThat(dto.getAvatarUrl()).isNull();
    }

    @Test
    void getCurrentUser_unknownUser_throwsNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser("ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_newUsername_savesAndReturnsTokens() {
        when(userRepository.existsByUsername("alicia")).thenReturn(false);
        when(jwtService.generateAccessToken(any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
        UpdateProfileRequestDTO request = new UpdateProfileRequestDTO();
        request.setUsername("  alicia ");

        AuthResponseDTO response = userService.updateProfile("alice", request);

        assertThat(response.getUsername()).isEqualTo("alicia");
        assertThat(response.getToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_sameUsername_doesNotSave() {
        UpdateProfileRequestDTO request = new UpdateProfileRequestDTO();
        request.setUsername("alice");

        userService.updateProfile("alice", request);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfile_takenUsername_throwsConflict() {
        when(userRepository.existsByUsername("bob")).thenReturn(true);
        UpdateProfileRequestDTO request = new UpdateProfileRequestDTO();
        request.setUsername("bob");

        assertThatThrownBy(() -> userService.updateProfile("alice", request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateAvatar_validImage_storesItAndReturnsUrl() {
        when(userAvatarRepository.findById(1L)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "me.png", "image/png", new byte[]{1, 2, 3});

        UserResponseDTO dto = userService.updateAvatar("alice", file);

        verify(userAvatarRepository).save(any(UserAvatar.class));
        assertThat(user.getAvatarVersion()).isNotNull();
        assertThat(dto.getAvatarUrl()).startsWith("/api/v1/users/1/avatar?v=");
    }

    @Test
    void updateAvatar_emptyFile_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "me.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> userService.updateAvatar("alice", file))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateAvatar_tooLarge_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "me.png", "image/png", new byte[3 * 1024 * 1024]);

        assertThatThrownBy(() -> userService.updateAvatar("alice", file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("2 MB");
    }

    @Test
    void updateAvatar_unsupportedType_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "me.gif", "image/gif", new byte[]{1});

        assertThatThrownBy(() -> userService.updateAvatar("alice", file))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deleteAvatar_removesImageAndClearsVersion() {
        user.setAvatarVersion(123L);
        UserAvatar avatar = new UserAvatar();
        when(userAvatarRepository.findById(1L)).thenReturn(Optional.of(avatar));

        userService.deleteAvatar("alice");

        verify(userAvatarRepository).delete(avatar);
        assertThat(user.getAvatarVersion()).isNull();
    }

    @Test
    void avatar_withObjectStorage_isUploadedReplacedAndServedByUrl() {
        when(objectStorage.getIfAvailable()).thenReturn(storage);
        UserAvatar avatar = new UserAvatar();
        avatar.setStorageKey("avatars/1/anterior");
        avatar.setContentType("image/png");
        when(userAvatarRepository.findById(1L)).thenReturn(Optional.of(avatar));
        MockMultipartFile file = new MockMultipartFile("file", "yo.png", "image/png", new byte[]{1, 2});

        userService.updateAvatar("alice", file);

        assertThat(avatar.getData()).isNull();
        assertThat(avatar.getStorageKey()).startsWith("avatars/1/").isNotEqualTo("avatars/1/anterior");
        verify(storage).put(org.mockito.ArgumentMatchers.startsWith("avatars/1/"),
                org.mockito.ArgumentMatchers.eq(new byte[]{1, 2}), org.mockito.ArgumentMatchers.eq("image/png"));
        verify(storage).delete("avatars/1/anterior");

        when(storage.temporaryUrl(avatar.getStorageKey(), "avatar", false)).thenReturn("https://s3/avatar");
        assertThat(userService.getAvatar(1L).redirectUrl()).isEqualTo("https://s3/avatar");

        userService.deleteAvatar("alice");
        verify(storage).delete(avatar.getStorageKey());
    }

    @Test
    void getAvatar_existing_returnsImage() {
        UserAvatar avatar = new UserAvatar();
        avatar.setData(new byte[]{7});
        avatar.setContentType("image/png");
        when(userAvatarRepository.findById(1L)).thenReturn(Optional.of(avatar));

        AvatarImageDTO image = userService.getAvatar(1L);

        assertThat(image.contentType()).isEqualTo("image/png");
        assertThat(image.data()).containsExactly(7);
    }

    @Test
    void getAvatar_missing_throwsNotFound() {
        when(userAvatarRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getAvatar(2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_togglesAdvisorProfile() {
        when(jwtService.generateAccessToken(any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdateProfileRequestDTO request = new UpdateProfileRequestDTO();
        request.setUsername("alice");
        request.setAdvisor(true);

        userService.updateProfile("alice", request);

        assertThat(user.isAdvisor()).isTrue();
        assertThat(userService.getCurrentUser("alice").isAdvisor()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_withCorrectCurrentPassword_savesNewHash() {
        when(passwordEncoder.matches("actual123", "hash")).thenReturn(true);
        when(passwordEncoder.matches("nueva123", "hash")).thenReturn(false);
        when(passwordEncoder.encode("nueva123")).thenReturn("hash-nuevo");

        userService.changePassword("alice", "actual123", "nueva123");

        assertThat(user.getPassword()).isEqualTo("hash-nuevo");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_withWrongOrRepeatedPassword_throwsBadRequest() {
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);
        when(passwordEncoder.matches("actual123", "hash")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword("alice", "mala", "nueva123"))
                .isInstanceOf(com.eduquest.api.exception.BadRequestException.class)
                .hasMessageContaining("actual");
        assertThatThrownBy(() -> userService.changePassword("alice", "actual123", "actual123"))
                .isInstanceOf(com.eduquest.api.exception.BadRequestException.class)
                .hasMessageContaining("distinta");
    }

    @Test
    void getPublicProfile_returnsEarnedBadgesAndFollowState() {
        User bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");
        bob.setAdvisor(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        com.eduquest.api.dto.response.GamificationStatsDTO stats =
                new com.eduquest.api.dto.response.GamificationStatsDTO(3, 1, 0, 0, 0, 0, 0);
        when(gamificationService.getProfile(2L)).thenReturn(new com.eduquest.api.dto.response.GamificationProfileDTO(
                35, 10, 4, stats, java.util.List.of(
                        new com.eduquest.api.dto.response.BadgeDTO("FIRST_UPLOAD", "Primer aporte", "d", true),
                        new com.eduquest.api.dto.response.BadgeDTO("CRITIC", "Crítico", "d", false))));
        when(userRepository.countFollowers(2L)).thenReturn(5L);
        when(userRepository.countFollowing(2L)).thenReturn(2L);
        when(userRepository.existsByIdAndFollowingId(1L, 2L)).thenReturn(true);

        com.eduquest.api.dto.response.PublicProfileDTO profile = userService.getPublicProfile("alice", 2L);

        assertThat(profile.username()).isEqualTo("bob");
        assertThat(profile.advisor()).isTrue();
        assertThat(profile.points()).isEqualTo(35);
        assertThat(profile.badges()).extracting("code").containsExactly("FIRST_UPLOAD");
        assertThat(profile.followers()).isEqualTo(5);
        assertThat(profile.followedByMe()).isTrue();
        assertThat(profile.me()).isFalse();
    }

    @Test
    void getPublicProfile_unknownUser_throwsNotFound() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getPublicProfile("alice", 9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPublicDocuments_delegatesToDocumentService() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 6);
        com.eduquest.api.dto.response.PageResponseDTO<com.eduquest.api.dto.response.DocumentResponseDTO> page =
                com.eduquest.api.dto.response.PageResponseDTO.of(java.util.List.of(), 0, 6, 0);
        when(documentService.getPublicDocumentsByAuthor(2L, pageable)).thenReturn(page);

        assertThat(userService.getPublicDocuments(2L, pageable)).isSameAs(page);
    }

    @Test
    void follow_addsUserAndRejectsSelf() {
        User bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));

        userService.follow("alice", 2L);

        assertThat(user.getFollowing()).containsExactly(bob);
        verify(userRepository).save(user);
        assertThatThrownBy(() -> userService.follow("alice", 1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void unfollow_removesUser_andListsFollowing() {
        User bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");
        user.getFollowing().add(bob);
        when(userRepository.findFollowing(1L)).thenReturn(java.util.List.of(bob));

        assertThat(userService.getFollowing("alice")).extracting("username").containsExactly("bob");

        userService.unfollow("alice", 2L);
        userService.unfollow("alice", 2L);

        assertThat(user.getFollowing()).isEmpty();
        verify(userRepository, org.mockito.Mockito.times(1)).save(user);
    }
}
