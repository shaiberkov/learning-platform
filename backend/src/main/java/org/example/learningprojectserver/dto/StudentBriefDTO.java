package org.example.learningprojectserver.dto;

/** A student as a teacher sees them in lists: id, name and class. */
public record StudentBriefDTO(String userId, String username, Long classRoomId, String className) {
}
