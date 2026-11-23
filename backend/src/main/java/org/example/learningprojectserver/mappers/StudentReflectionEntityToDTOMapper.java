package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.WellbeingDTOs;
import org.example.learningprojectserver.entities.StudentReflectionEntity;
import org.springframework.stereotype.Component;

/** The stored reflection; the service adds the measured progress and the goal status. */
@Component
public class StudentReflectionEntityToDTOMapper implements Mapper<StudentReflectionEntity, WellbeingDTOs.Reflection> {
    @Override
    public WellbeingDTOs.Reflection apply(StudentReflectionEntity r) {
        return new WellbeingDTOs.Reflection(r.getId(), r.getWeekStart(), r.getWentWell(), r.getStuckOn(),
                r.getNextGoal(), r.getGoalType(), r.getGoalTarget(), r.getGoalProgress(), r.getAchieved(), null);
    }
}
