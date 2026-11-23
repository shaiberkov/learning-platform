package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.StudySheetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudySheetRepository extends JpaRepository<StudySheetEntity, Long> {

    List<StudySheetEntity> findTop30ByStudentUserIdOrderByCreatedAtDesc(String studentUserId);

    Optional<StudySheetEntity> findByIdAndStudentUserId(Long id, String studentUserId);
}
