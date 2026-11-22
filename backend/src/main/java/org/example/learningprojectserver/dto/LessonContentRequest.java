package org.example.learningprojectserver.dto;

import java.util.List;

/** Body of create/update lesson page. videoUrl may be any YouTube or Vimeo link, or empty. */
public record LessonContentRequest(String title,
                                   String subject,
                                   String topic,
                                   String subTopic,
                                   String body,
                                   String videoUrl,
                                   List<String> examples) {
}
