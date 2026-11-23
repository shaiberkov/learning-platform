package org.example.learningprojectserver.dto;

public record AccommodationDTO(String studentUserId,
                               String studentName,
                               String className,
                               int extraTimePercent,
                               boolean readAloud,
                               boolean dyslexiaFont) {
}
