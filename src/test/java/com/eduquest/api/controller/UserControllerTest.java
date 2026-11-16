package com.eduquest.api.controller;

import com.eduquest.api.dto.request.UpdateProfileRequestDTO;
import com.eduquest.api.dto.response.AuthResponseDTO;
import com.eduquest.api.dto.response.AvatarImageDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.security.UserPrincipal;
import com.eduquest.api.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {

    private final UserService userService = mock(UserService.class);
    private final UserController userController = new UserController(userService);
    private final UserPrincipal principal = new UserPrincipal(1L, "alice", "alice@utec.edu.pe", "hash",
            List.of(new SimpleGrantedAuthority("ROLE_USER")));

    @Test
    void getCurrentUser_delegatesToService() {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(1L);
        dto.setUsername("alice");
        dto.setRoles(Set.of("ROLE_USER"));
        when(userService.getCurrentUser("alice")).thenReturn(dto);

        ResponseEntity<UserResponseDTO> result = userController.getCurrentUser(principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getUsername()).isEqualTo("alice");
    }

    @Test
    void updateProfile_returnsNewTokens() {
        UpdateProfileRequestDTO request = new UpdateProfileRequestDTO();
        request.setUsername("alice2");
        AuthResponseDTO auth = new AuthResponseDTO("access", "refresh", 1L, "alice2", "alice@utec.edu.pe", Set.of("ROLE_USER"));
        when(userService.updateProfile("alice", request)).thenReturn(auth);

        ResponseEntity<AuthResponseDTO> result = userController.updateProfile(request, principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getUsername()).isEqualTo("alice2");
    }

    @Test
    void updateAvatar_delegatesFileToService() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});
        UserResponseDTO dto = new UserResponseDTO();
        dto.setAvatarUrl("/api/v1/users/1/avatar?v=1");
        when(userService.updateAvatar("alice", file)).thenReturn(dto);

        ResponseEntity<UserResponseDTO> result = userController.updateAvatar(file, principal);

        assertThat(result.getBody().getAvatarUrl()).isEqualTo("/api/v1/users/1/avatar?v=1");
    }

    @Test
    void deleteAvatar_returnsNoContent() {
        ResponseEntity<Void> result = userController.deleteAvatar(principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).deleteAvatar("alice");
    }

    @Test
    void getAvatar_returnsImageWithContentType() {
        when(userService.getAvatar(1L)).thenReturn(new AvatarImageDTO(new byte[]{9, 9}, "image/jpeg"));

        ResponseEntity<byte[]> result = userController.getAvatar(1L);

        assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
        assertThat(result.getBody()).containsExactly(9, 9);
    }

    @Test
    void changePassword_delegatesToService() {
        com.eduquest.api.dto.request.ChangePasswordRequestDTO request = new com.eduquest.api.dto.request.ChangePasswordRequestDTO();
        request.setCurrentPassword("actual123");
        request.setNewPassword("nueva123");

        assertThat(userController.changePassword(request, principal).getStatusCode())
                .isEqualTo(org.springframework.http.HttpStatus.NO_CONTENT);
        verify(userService).changePassword("alice", "actual123", "nueva123");
    }

    @Test
    void profileAndFollowEndpoints_delegateToService() {
        com.eduquest.api.dto.response.PublicProfileDTO profile = new com.eduquest.api.dto.response.PublicProfileDTO(
                2L, "bob", null, false, null, 0, 0, null, null, List.of(), 0, 0, false, false);
        when(userService.getPublicProfile("alice", 2L)).thenReturn(profile);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 6);
        com.eduquest.api.dto.response.PageResponseDTO<com.eduquest.api.dto.response.DocumentResponseDTO> page =
                com.eduquest.api.dto.response.PageResponseDTO.of(List.of(), 0, 6, 0);
        when(userService.getPublicDocuments(2L, pageable)).thenReturn(page);
        when(userService.getFollowing("alice")).thenReturn(List.of());

        assertThat(userController.getPublicProfile(2L, principal).getBody()).isSameAs(profile);
        assertThat(userController.getPublicDocuments(2L, pageable).getBody()).isSameAs(page);
        assertThat(userController.getFollowing(principal).getBody()).isEmpty();
        assertThat(userController.follow(2L, principal).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(userController.unfollow(2L, principal).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).follow("alice", 2L);
        verify(userService).unfollow("alice", 2L);
    }
}
