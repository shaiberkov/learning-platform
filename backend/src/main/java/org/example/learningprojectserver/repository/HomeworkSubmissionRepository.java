package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.HomeworkSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HomeworkSubmissionRepository extends JpaRepository<HomeworkSubmissionEntity, Long> {
    List<HomeworkSubmissionEntity> findByHomeworkIdIn(Collection<Long> homeworkIds);

    Optional<HomeworkSubmissionEntity> findFirstByHomeworkIdAndStudentUserId(Long homeworkId, String studentUserId);

    long countByStudentUserIdAndLateFalse(String studentUserId);
}
