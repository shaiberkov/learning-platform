package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A spoken study summary ("podcast", about five minutes) built from a test's topics and the
 * student's mistakes. The script is stored; its AI voice recording, once made, is in
 * {@link StudyPodcastAudioEntity} so listing podcasts never loads audio bytes.
 */
@Entity
@Table(name = "study_podcast", indexes = @Index(name = "idx_study_podcast_student", columnList = "studentUserId,createdAt"))
@Getter
@Setter
@NoArgsConstructor
public class StudyPodcastEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentUserId;

    @Column(nullable = false)
    private String title;

    private Long testId;

    @Column(length = 1000)
    private String topics;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String script;

    @Column(nullable = false)
    private boolean aiGenerated;

    @Column(nullable = false)
    private boolean hasAudio;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
