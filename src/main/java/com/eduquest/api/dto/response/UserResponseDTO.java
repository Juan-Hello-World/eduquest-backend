package com.eduquest.api.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class UserResponseDTO {
    private Long id;
    private String username;
    private String email;
    private Set<String> roles;
    private String avatarUrl;
    private boolean advisor;
    private boolean emailVerified = true;
    private boolean banned;
    private String banReason;
    private LocalDateTime bannedAt;
    private LocalDateTime bannedUntil;
    private String availability;
    private String statusText;
    private String statusEmoji;

    public static String avatarUrlFor(Long userId, Long avatarVersion) {
        return avatarVersion == null ? null : "/api/v1/users/" + userId + "/avatar?v=" + avatarVersion;
    }
}
