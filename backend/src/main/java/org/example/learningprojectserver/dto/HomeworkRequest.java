package org.example.learningprojectserver.dto;

import java.util.List;

/** Body of create homework. dueDate is ISO local date-time (yyyy-MM-ddTHH:mm), Israel time. */
public record HomeworkRequest(String title,
                              String description,
                              String subject,
                              String dueDate,
                              List<Long> classRoomIds) {
}
