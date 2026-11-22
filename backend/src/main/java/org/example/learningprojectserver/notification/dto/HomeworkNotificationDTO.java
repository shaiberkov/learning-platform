package org.example.learningprojectserver.notification.dto;

/** Payload of NEW_HOMEWORK and HOMEWORK_REMINDER. dueDate is formatted dd/MM/yyyy HH:mm. */
public record HomeworkNotificationDTO(Long homeworkId, String title, String dueDate, String message) {
}
