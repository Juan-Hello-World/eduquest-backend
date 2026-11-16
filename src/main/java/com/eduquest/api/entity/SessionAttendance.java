package com.eduquest.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_attendances", uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class SessionAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private StudySession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed")
    private Boolean confirmed = true;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    public SessionAttendance(StudySession session, User user, LocalDateTime createdAt) {
        this.session = session;
        this.user = user;
        this.createdAt = createdAt;
    }

    public boolean isConfirmed() {
        return !Boolean.FALSE.equals(confirmed);
    }

    public boolean isCheckedIn() {
        return checkedInAt != null;
    }
}
