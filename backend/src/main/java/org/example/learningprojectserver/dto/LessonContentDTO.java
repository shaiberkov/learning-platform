package org.example.learningprojectserver.dto;

import java.time.LocalDateTime;
import java.util.List;

/** A lesson page. The progress flags are the viewing student's; they are false for teachers. */
public record LessonContentDTO(Long id,
                               String teacherUserId,
                               String title,
                               String subject,
                               String topic,
                               String subTopic,
                               String body,
                               String videoEmbedUrl,
                               List<String> examples,
                               LocalDateTime updatedAt,
                               boolean learned,
                               boolean practiced,
                               boolean reviewed) {
}
