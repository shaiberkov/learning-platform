package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.BankQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankQuestionRepository extends JpaRepository<BankQuestionEntity, Long> {

    List<BankQuestionEntity> findByOwnerTeacherUserIdOrderByIdDesc(String ownerTeacherUserId);
}
