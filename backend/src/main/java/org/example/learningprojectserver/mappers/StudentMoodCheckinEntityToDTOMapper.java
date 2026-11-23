package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.WellbeingDTOs;
import org.example.learningprojectserver.entities.StudentMoodCheckinEntity;
import org.springframework.stereotype.Component;

@Component
public class StudentMoodCheckinEntityToDTOMapper implements Mapper<StudentMoodCheckinEntity, WellbeingDTOs.Checkin> {
    @Override
    public WellbeingDTOs.Checkin apply(StudentMoodCheckinEntity c) {
        return new WellbeingDTOs.Checkin(c.getCheckinDate(), c.getMood(), c.getExamLabel());
    }
}
