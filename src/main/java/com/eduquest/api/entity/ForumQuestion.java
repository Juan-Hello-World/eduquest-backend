package com.eduquest.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "forum_questions", indexes = @Index(name = "idx_forum_questions_created", columnList = "created_at"))
@Getter
@Setter
@NoArgsConstructor
public class ForumQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(length = 100)
    private String course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "accepted_answer_id")
    private Long acceptedAnswerId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
