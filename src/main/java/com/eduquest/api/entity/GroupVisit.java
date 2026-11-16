package com.eduquest.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_visits", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "group_id"}))
@Getter
@Setter
@NoArgsConstructor
public class GroupVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private PrivateGroup group;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "chat_seen_at")
    private LocalDateTime chatSeenAt;

    @Column(name = "muted")
    private Boolean muted = false;

    public boolean isMuted() {
        return Boolean.TRUE.equals(muted);
    }

    public GroupVisit(User user, PrivateGroup group, LocalDateTime lastSeenAt) {
        this.user = user;
        this.group = group;
        this.lastSeenAt = lastSeenAt;
    }
}
