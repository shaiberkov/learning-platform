package org.example.learningprojectserver.dto;

/** Body of the create-class-challenge request. Dates are ISO (yyyy-MM-dd). */
public record ClassChallengeRequest(Long classRoomId, String title, int targetXp, String startDate, String endDate) {
}
