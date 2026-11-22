package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.exam.TeacherExamSummary;
import org.example.learningprojectserver.entities.TeacherTestEntity;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/** Teacher exam row without the submitted count, which the service adds. Needs an open session. */
@Component
public class TeacherTestToExamSummaryMapper implements Mapper<TeacherTestEntity, TeacherExamSummary> {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public TeacherExamSummary apply(TeacherTestEntity test) {
        TeacherExamSummary s = new TeacherExamSummary();
        s.setTestId(test.getId());
        s.setTitle(test.getTitle() != null ? test.getTitle() : test.getSubject() + " - " + test.getTopic());
        s.setSubject(test.getSubject());
        s.setTopic(test.getTopic());
        s.setStartTime(test.getStartTime());
        s.setTimeLimitMinutes(test.getTimeLimitMinutes());
        s.setPublished(test.isPublishedNow());
        s.setPublishAt(test.getPublishAt() == null ? null : test.getPublishAt().format(FORMAT));
        s.setTemplate(test.isTemplateExam());
        s.setVersionLabel(test.getVersionLabel());
        s.setExamGroupId(test.getExamGroupId());
        s.setSourceTestId(test.getSourceTestId());
        s.setQuestionCount(test.getQuestions().size());
        s.setStudentCount(test.getStudents().size());
        return s;
    }
}
