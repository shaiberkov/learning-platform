package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.GradebookWeightEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradebookWeightRepository extends JpaRepository<GradebookWeightEntity, Long> {
    List<GradebookWeightEntity> findByTeacherIdAndClassRoomId(String teacherId, Long classRoomId);
}
