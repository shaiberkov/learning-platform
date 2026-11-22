package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.ClassChallengeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassChallengeRepository extends JpaRepository<ClassChallengeEntity, Long> {
    List<ClassChallengeEntity> findByTeacherUserIdOrderByEndDateDesc(String teacherUserId);

    List<ClassChallengeEntity> findByClassRoomIdOrderByEndDateDesc(Long classRoomId);
}
