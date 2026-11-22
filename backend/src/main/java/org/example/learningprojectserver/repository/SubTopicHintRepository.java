package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.SubTopicHintEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubTopicHintRepository extends JpaRepository<SubTopicHintEntity, Long> {

    Optional<SubTopicHintEntity> findBySubjectAndSubTopic(String subject, String subTopic);

    List<SubTopicHintEntity> findBySubject(String subject);
}
