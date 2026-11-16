package com.eduquest.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "study_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    private String meetingUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private PrivateGroup privateGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "recording_url")
    private String recordingUrl;

    @Column(name = "recording_title")
    private String recordingTitle;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SessionAttendance> attendances = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SessionReview> reviews = new ArrayList<>();

    public static final int DEFAULT_DURATION_MINUTES = 60;
    public static final int CHECK_IN_EARLY_MINUTES = 10;

    public LocalDateTime getEffectiveEnd() {
        if (endsAt != null) {
            return endsAt;
        }
        return scheduledAt != null ? scheduledAt.plusMinutes(DEFAULT_DURATION_MINUTES) : null;
    }

    public boolean hasStarted() {
        return scheduledAt != null && !scheduledAt.isAfter(LocalDateTime.now());
    }

    public boolean isFinished() {
        LocalDateTime end = getEffectiveEnd();
        return end != null && end.isBefore(LocalDateTime.now());
    }

    public boolean isCheckInOpen() {
        LocalDateTime now = LocalDateTime.now();
        return scheduledAt != null
                && !now.isBefore(scheduledAt.minusMinutes(CHECK_IN_EARLY_MINUTES))
                && !isFinished();
    }
}
