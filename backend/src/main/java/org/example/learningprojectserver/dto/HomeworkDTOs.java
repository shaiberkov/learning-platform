package org.example.learningprojectserver.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Response shapes of the homework endpoints. */
public final class HomeworkDTOs {
    private HomeworkDTOs() {}

    /** SUBMITTED, LATE (submitted after the due date), MISSING (not submitted, past due) or PENDING (not yet due). */
    public record StudentHomework(Long id,
                                  String title,
                                  String description,
                                  String subject,
                                  LocalDateTime dueDate,
                                  String status,
                                  String submittedContent,
                                  LocalDateTime submittedAt) {
    }

    public record BoardRow(String studentUserId,
                           String studentName,
                           String className,
                           String status,
                           LocalDateTime submittedAt,
                           String content) {
    }

    public record BoardHomework(Long id,
                                String title,
                                String description,
                                String subject,
                                LocalDateTime dueDate,
                                boolean reminderSent,
                                int assigned,
                                int submitted,
                                int late,
                                List<BoardRow> students) {
    }

    public record TeacherClass(Long classRoomId, String className, int studentCount) {
    }
}
