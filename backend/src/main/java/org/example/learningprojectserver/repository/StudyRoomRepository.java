package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.StudyRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyRoomRepository extends JpaRepository<StudyRoomEntity, Long> {

    List<StudyRoomEntity> findBySchoolCodeAndActiveTrueOrderByCreatedAtDesc(String schoolCode);
}
