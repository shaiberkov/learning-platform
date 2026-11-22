package org.example.learningprojectserver.service.ai.insights;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.entities.TestAnswerRecordEntity;
import org.example.learningprojectserver.entities.TestQuestionEntity;
import org.example.learningprojectserver.repository.TestAnswerRecordRepository;
import org.example.learningprojectserver.service.ai.AiGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Keeps each answer of a checked teacher test, for the weekly insights. Called from
 * {@code TestResultService.checkTeacherTest}; a failure here is logged and never fails the submit.
 */
@Component
@RequiredArgsConstructor
public class TestAnswerRecorder {

    private static final Logger log = LoggerFactory.getLogger(TestAnswerRecorder.class);

    private final TestAnswerRecordRepository repository;

    public void record(Long testId, String teacherUserId, String studentUserId,
                       List<TestQuestionEntity> questions, Map<Long, String> answers) {
        try {
            List<TestAnswerRecordEntity> rows = new ArrayList<>();
            for (TestQuestionEntity question : questions) {
                String answer = answers.get(question.getId());
                String given = answer == null ? "" : answer.trim();
                TestAnswerRecordEntity row = new TestAnswerRecordEntity();
                row.setTestId(testId);
                row.setQuestionId(question.getId());
                row.setTeacherUserId(teacherUserId);
                row.setStudentUserId(studentUserId);
                row.setTopic(question.getTopic());
                row.setSubTopic(question.getSubTopic());
                row.setQuestionText(question.getQuestionText());
                row.setCorrectAnswer(question.getAnswer());
                row.setAnswer(truncate(given));
                // Same comparison as TestResultService.checkTeacherTest.
                row.setCorrect(answer != null && answer.equalsIgnoreCase(question.getAnswer().trim()));
                row.setCreatedAt(AiGateway.now());
                rows.add(row);
            }
            repository.saveAll(rows);
        } catch (RuntimeException e) {
            log.warn("Could not record answers of test {} for {}: {}", testId, studentUserId, e.getMessage());
        }
    }

    private static String truncate(String s) {
        return s.length() <= 255 ? s : s.substring(0, 255);
    }
}
