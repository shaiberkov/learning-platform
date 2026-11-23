package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.exam.ItemAnalysisView;
import org.example.learningprojectserver.entities.TeacherTestEntity;
import org.example.learningprojectserver.entities.TestQuestionEntity;
import org.example.learningprojectserver.enums.ExamAttemptStatus;
import org.example.learningprojectserver.enums.QuestionKind;
import org.example.learningprojectserver.repository.ExamAttemptAnswerRepository;
import org.example.learningprojectserver.repository.TeacherTestRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.exam.ItemAnalysisCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Per-question difficulty and discrimination for one exam, from its graded attempts. */
@Service
@RequiredArgsConstructor
public class ItemAnalysisService {

    private final TeacherTestRepository teacherTestRepository;
    private final ExamAttemptAnswerRepository examAttemptAnswerRepository;

    @Transactional(readOnly = true)
    public BasicResponse<ItemAnalysisView> analyse(String teacherId, Long testId) {
        TeacherTestEntity test = teacherTestRepository.findTeacherTestByTestId(testId);
        if (test == null || test.getTeacher() == null || !test.getTeacher().getUserId().equals(teacherId)) {
            return new BasicResponse<>(false, "המבחן לא נמצא או שאינו שלך");
        }
        List<ItemAnalysisCalculator.Question> questions = new ArrayList<>();
        int position = 1;
        for (TestQuestionEntity q : ExamAttemptService.orderedQuestions(test)) {
            QuestionKind kind = QuestionKind.of(q.getKind());
            questions.add(new ItemAnalysisCalculator.Question(q.getId(), position++, q.getDisplayText(),
                    kind == null ? QuestionKind.TEXT.name() : kind.name()));
        }
        List<ItemAnalysisCalculator.Row> rows = new ArrayList<>();
        for (Object[] r : examAttemptAnswerRepository.findGradedRowsForTest(testId, ExamAttemptStatus.IN_PROGRESS)) {
            rows.add(new ItemAnalysisCalculator.Row((Long) r[0], (Integer) r[1], (Long) r[2], (Boolean) r[3]));
        }
        int examinees = (int) rows.stream().map(ItemAnalysisCalculator.Row::attemptId).distinct().count();

        ItemAnalysisView view = new ItemAnalysisView();
        view.setTestId(testId);
        view.setTitle(test.getTitle() != null ? test.getTitle() : test.getSubject() + " - " + test.getTopic());
        view.setExamineeCount(examinees);
        view.setGroupSize(ItemAnalysisCalculator.groupSize(examinees));
        view.setItems(ItemAnalysisCalculator.analyse(questions, rows));

        BasicResponse<ItemAnalysisView> response = new BasicResponse<>(true, null);
        response.setData(view);
        return response;
    }
}
