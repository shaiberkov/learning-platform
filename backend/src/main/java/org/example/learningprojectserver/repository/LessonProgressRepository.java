package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.LessonProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgressEntity, Long> {
    Optional<LessonProgressEntity> findFirstByStudentUserIdAndLessonContentId(String studentUserId, Long lessonContentId);

    List<LessonProgressEntity> findByStudentUserId(String studentUserId);

    void deleteByLessonContentId(Long lessonContentId);
}
