package org.example.learningprojectserver.service.ai.tutor;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.StudentTestStatusDTO;
import org.example.learningprojectserver.repository.StudentRepository;
import org.example.learningprojectserver.repository.TeacherTestRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Server-side rule: the AI never helps a student during an exam. A student is "in an exam" from a
 * teacher test's start time until its time limit ends, as long as they have not submitted it.
 * Questions that belong to a teacher test are off limits at any time.
 * Times use the server clock, like {@code TestResultService.startTest}.
 */
@Component
@RequiredArgsConstructor
public class ExamGuard {

    public static final String EXAM_ACTIVE_MESSAGE = "יש לך מבחן פעיל כרגע, ולכן עוזר ה-AI כבוי עד סוף המבחן. בהצלחה!";
    public static final String EXAM_QUESTION_MESSAGE = "זו שאלה ממבחן, ועוזר ה-AI לא עוזר בשאלות מבחן.";

    private static final DateTimeFormatter TEST_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final StudentRepository studentRepository;
    private final TeacherTestRepository teacherTestRepository;

    public boolean isInActiveExam(String studentUserId) {
        return hasActiveExam(studentRepository.findAllTestsForStudent(studentUserId), LocalDateTime.now());
    }

    public boolean isExamQuestion(Long questionId) {
        return questionId != null && teacherTestRepository.isTeacherTestQuestion(questionId);
    }

    static boolean hasActiveExam(List<StudentTestStatusDTO> tests, LocalDateTime now) {
        for (StudentTestStatusDTO test : tests) {
            if (test.getScore() != -1 || test.getStartTime() == null) {
                continue; // already submitted (or graded as missed)
            }
            try {
                LocalDateTime start = LocalDateTime.parse(test.getStartTime(), TEST_TIME);
                LocalDateTime end = start.plusMinutes(test.getTimeLimitMinutes());
                if (!now.isBefore(start) && now.isBefore(end)) {
                    return true;
                }
            } catch (DateTimeParseException ignored) {
                // A malformed start time cannot describe an active exam.
            }
        }
        return false;
    }
}
