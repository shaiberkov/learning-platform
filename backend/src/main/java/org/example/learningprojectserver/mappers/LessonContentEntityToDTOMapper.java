package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.LessonContentDTO;
import org.example.learningprojectserver.entities.LessonContentEntity;
import org.example.learningprojectserver.entities.LessonProgressEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/** Lesson page + the viewing student's progress (null for teachers) to its DTO. */
@Component
public class LessonContentEntityToDTOMapper
        implements Mapper<LessonContentEntityToDTOMapper.LessonWithProgress, LessonContentDTO> {

    public record LessonWithProgress(LessonContentEntity lesson, LessonProgressEntity progress) {
    }

    @Override
    public LessonContentDTO apply(LessonWithProgress input) {
        LessonContentEntity l = input.lesson();
        LessonProgressEntity p = input.progress();
        return new LessonContentDTO(
                l.getId(),
                l.getTeacherUserId(),
                l.getTitle(),
                l.getSubject(),
                l.getTopic(),
                l.getSubTopic(),
                l.getBody(),
                l.getVideoEmbedUrl(),
                List.copyOf(l.getExamples()),
                l.getUpdatedAt(),
                p != null && p.isLearned(),
                p != null && p.isPracticed(),
                p != null && p.isReviewed());
    }
}
