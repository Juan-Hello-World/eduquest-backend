package com.eduquest.api.entity;

import jakarta.persistence.*;
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
@Table(name = "private_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrivateGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(unique = true, length = 8)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private GroupVisibility visibility = GroupVisibility.PUBLIC;

    @Column(name = "join_password")
    private String joinPassword;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "invite_token", length = 40)
    private String inviteToken;

    @Column(name = "invite_expires_at")
    private LocalDateTime inviteExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members = new HashSet<>();

    public boolean isPrivate() {
        return visibility == GroupVisibility.PRIVATE;
    }
}