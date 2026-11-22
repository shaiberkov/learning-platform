package org.example.learningprojectserver.dto;

import java.time.LocalDate;

/** A class challenge with the class's combined progress. No per-student breakdown on purpose. */
public record ClassChallengeDTO(Long id,
                                String title,
                                Long classRoomId,
                                String className,
                                int targetXp,
                                long classXp,
                                int percent,
                                LocalDate startDate,
                                LocalDate endDate,
                                String status) {
}
