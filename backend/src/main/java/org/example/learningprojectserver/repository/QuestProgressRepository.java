package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.QuestProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestProgressRepository extends JpaRepository<QuestProgressEntity, Long> {

    List<QuestProgressEntity> findByStudentUserId(String studentUserId);

    Optional<QuestProgressEntity> findByStudentUserIdAndQuestCode(String studentUserId, String questCode);
}
