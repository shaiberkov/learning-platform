package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.StudyPlanDTO;
import org.example.learningprojectserver.entities.StudyPlanTaskEntity;
import org.springframework.stereotype.Component;

@Component
public class StudyPlanTaskEntityToTaskMapper implements Mapper<StudyPlanTaskEntity, StudyPlanDTO.Task> {
    @Override
    public StudyPlanDTO.Task apply(StudyPlanTaskEntity t) {
        return new StudyPlanDTO.Task(t.getId(), t.getTestId(), t.getTaskDate(), t.getSubject(), t.getTopic(),
                t.getMinutes(), t.getTaskType(), t.getDescription(), t.isCompleted());
    }
}
