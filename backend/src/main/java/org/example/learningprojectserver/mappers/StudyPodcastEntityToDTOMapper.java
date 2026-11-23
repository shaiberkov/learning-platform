package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.StudySummaryDTOs.Podcast;
import org.example.learningprojectserver.dto.StudySummaryDTOs.PodcastSummary;
import org.example.learningprojectserver.entities.StudyPodcastEntity;
import org.springframework.stereotype.Component;

@Component
public class StudyPodcastEntityToDTOMapper implements Mapper<StudyPodcastEntity, Podcast> {

    /** Hebrew read aloud at a relaxed pace, words per minute. */
    static final int WORDS_PER_MINUTE = 130;

    @Override
    public Podcast apply(StudyPodcastEntity p) {
        return new Podcast(p.getId(), p.getTitle(), p.getTopics(), p.getTestId(), p.isAiGenerated(), p.isHasAudio(),
                estimatedMinutes(p.getScript()), p.getCreatedAt(), p.getScript());
    }

    public PodcastSummary toSummary(StudyPodcastEntity p) {
        return new PodcastSummary(p.getId(), p.getTitle(), p.getTopics(), p.isAiGenerated(), p.isHasAudio(), p.getCreatedAt());
    }

    static int estimatedMinutes(String script) {
        if (script == null || script.isBlank()) {
            return 0;
        }
        int words = script.trim().split("\\s+").length;
        return Math.max(1, Math.round(words / (float) WORDS_PER_MINUTE));
    }
}
