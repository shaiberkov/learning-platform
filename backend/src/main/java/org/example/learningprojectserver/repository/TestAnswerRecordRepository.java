package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.TestAnswerRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TestAnswerRecordRepository extends JpaRepository<TestAnswerRecordEntity, Long> {

    List<TestAnswerRecordEntity> findByTeacherUserIdAndCreatedAtBetween(String teacherUserId, LocalDateTime from, LocalDateTime to);
}
