package org.example.learningprojectserver.dto;

import java.time.LocalDate;
import java.util.List;

/** A student's study plans for upcoming exams, today's tasks first. */
public record StudyPlanDTO(List<Task> today, List<ExamPlan> exams) {

    public record Task(Long id,
                       Long testId,
                       LocalDate date,
                       String subject,
                       String topic,
                       int minutes,
                       String taskType,
                       String description,
                       boolean completed) {
    }

    /** One exam with its tasks and how well the student kept to the plan so far. */
    public record ExamPlan(Long testId,
                           String subject,
                           String topic,
                           String examStartTime,
                           int daysLeft,
                           int dueTasks,
                           int completedDueTasks,
                           int adherencePercent,
                           List<Task> tasks) {
    }
}
