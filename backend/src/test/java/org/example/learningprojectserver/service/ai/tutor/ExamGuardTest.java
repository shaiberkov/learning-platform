package org.example.learningprojectserver.service.ai.tutor;

import org.example.learningprojectserver.dto.StudentTestStatusDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExamGuardTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 10, 10, 30);

    private static StudentTestStatusDTO test(String start, int minutes, int score) {
        StudentTestStatusDTO dto = new StudentTestStatusDTO();
        dto.setStartTime(start);
        dto.setTimeLimitMinutes(minutes);
        dto.setScore(score);
        return dto;
    }

    @Test
    void unsubmittedTestInsideItsWindowLocksTheTutor() {
        assertTrue(ExamGuard.hasActiveExam(List.of(test("10/03/2026 10:00", 45, -1)), NOW));
    }

    @Test
    void submittedTestDoesNotLock() {
        assertFalse(ExamGuard.hasActiveExam(List.of(test("10/03/2026 10:00", 45, 80)), NOW));
    }

    @Test
    void testsBeforeOrAfterTheWindowDoNotLock() {
        assertFalse(ExamGuard.hasActiveExam(List.of(test("10/03/2026 11:00", 45, -1)), NOW));
        assertFalse(ExamGuard.hasActiveExam(List.of(test("10/03/2026 09:00", 30, -1)), NOW));
    }

    @Test
    void malformedStartTimeIsIgnored() {
        assertFalse(ExamGuard.hasActiveExam(List.of(test("not a date", 45, -1)), NOW));
    }
}
