package com.eduquest.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "pinned_chats", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "kind", "target_id"}))
@Getter
@Setter
@NoArgsConstructor
public class PinnedChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PinnedChatKind kind;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "pinned_at", nullable = false)
    private LocalDateTime pinnedAt;
}
