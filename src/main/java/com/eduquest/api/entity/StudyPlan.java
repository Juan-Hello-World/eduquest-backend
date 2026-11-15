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
@Table(name = "study_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String courseName;

    @NotBlank
    @Column(nullable = false)
    private String difficulty;

    @Column(name = "generated_content", nullable = false, columnDefinition = "TEXT")
    private String generatedContent;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ElementCollection
    @CollectionTable(name = "study_plan_completed_steps", joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "step_index", nullable = false)
    private Set<Integer> completedSteps = new HashSet<>();
}