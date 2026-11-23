package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** The AI voice recording of one {@link StudyPodcastEntity}, kept so it is produced (and paid for) once. */
@Entity
@Table(name = "study_podcast_audio")
@Getter
@Setter
@NoArgsConstructor
public class StudyPodcastAudioEntity {

    /** Same id as the podcast. */
    @Id
    private Long podcastId;

    @Lob
    @Column(columnDefinition = "LONGBLOB", nullable = false)
    private byte[] audio;

    @Column(nullable = false, length = 64)
    private String contentType;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
