package com.eduquest.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_channels")
@Getter
@Setter
@NoArgsConstructor
public class GroupChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private PrivateGroup group;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(length = 200)
    private String topic;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_only")
    private Boolean readOnly = false;

    @Column(name = "slow_mode_seconds")
    private Integer slowModeSeconds = 0;

    public boolean isReadOnly() {
        return Boolean.TRUE.equals(readOnly);
    }

    public int slowMode() {
        return slowModeSeconds == null ? 0 : slowModeSeconds;
    }
}
