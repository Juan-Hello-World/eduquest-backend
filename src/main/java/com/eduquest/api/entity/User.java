package com.eduquest.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Column(name = "avatar_version")
    private Long avatarVersion;

    @Column(name = "banned")
    private Boolean banned = false;

    @Column(name = "ban_reason", columnDefinition = "TEXT")
    private String banReason;

    @Column(name = "banned_at")
    private LocalDateTime bannedAt;

    @Column(name = "banned_until")
    private LocalDateTime bannedUntil;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "status_text", length = 80)
    private String statusText;

    @Column(name = "status_emoji", length = 16)
    private String statusEmoji;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private Availability availability;

    @Column(name = "advisor")
    private Boolean advisor = false;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    @Column(name = "verification_code_hash")
    private String verificationCodeHash;

    @Column(name = "verification_expires_at")
    private LocalDateTime verificationExpiresAt;

    @Column(name = "verification_sent_at")
    private LocalDateTime verificationSentAt;

    @Column(name = "verification_attempts")
    private Integer verificationAttempts;

    @Column(name = "reset_code_hash")
    private String resetCodeHash;

    @Column(name = "reset_code_expires_at")
    private LocalDateTime resetCodeExpiresAt;

    @Column(name = "reset_attempts")
    private Integer resetAttempts;

    @ElementCollection
    @CollectionTable(name = "user_badges", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "badge_code", length = 30)
    private Set<String> earnedBadges = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "saved_documents",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "document_id")
    )
    private Set<Document> savedDocuments = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_follows",
            joinColumns = @JoinColumn(name = "follower_id"),
            inverseJoinColumns = @JoinColumn(name = "followed_id")
    )
    private Set<User> following = new HashSet<>();

    public boolean isBanned() {
        return Boolean.TRUE.equals(banned) && (bannedUntil == null || bannedUntil.isAfter(LocalDateTime.now()));
    }

    public boolean isEmailVerified() {
        return !Boolean.FALSE.equals(emailVerified);
    }

    public boolean isAdvisor() {
        return Boolean.TRUE.equals(advisor);
    }

    public boolean hasRole(RoleName roleName) {
        return roles != null && roles.stream().anyMatch(role -> role.getName() == roleName);
    }
}