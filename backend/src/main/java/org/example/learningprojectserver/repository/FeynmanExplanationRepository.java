package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.FeynmanExplanationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** [s06] History of the "explain it to me" (Feynman) attempts. */
@Repository
public interface FeynmanExplanationRepository extends JpaRepository<FeynmanExplanationEntity, Long> {

    List<FeynmanExplanationEntity> findTop100ByStudentUserIdOrderByCreatedAtDescIdDesc(String studentUserId);

    List<FeynmanExplanationEntity> findTop200ByStudentUserIdInOrderByCreatedAtDescIdDesc(List<String> studentUserIds);
}
