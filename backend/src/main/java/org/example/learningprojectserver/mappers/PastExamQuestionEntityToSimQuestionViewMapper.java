package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.examsim.ExamSimDTOs.SimQuestionView;
import org.example.learningprojectserver.entities.PastExamQuestionEntity;
import org.example.learningprojectserver.service.examsim.ExamSimGrader;
import org.springframework.stereotype.Component;

/** A past-exam question as the student sees it during a simulation: no answer, no rubric. */
@Component
public class PastExamQuestionEntityToSimQuestionViewMapper implements Mapper<PastExamQuestionEntity, SimQuestionView> {

    /** The kind the exam screen's answer control uses for free text. */
    static final String KIND = "TEXT";

    @Override
    public SimQuestionView apply(PastExamQuestionEntity q) {
        return new SimQuestionView(q.getId(), q.getPosition(), q.getText(), q.getPoints(),
                ExamSimGrader.answerHint(q.getAnswer()), KIND);
    }
}
