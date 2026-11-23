package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.StudyPodcastEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyPodcastRepository extends JpaRepository<StudyPodcastEntity, Long> {

    List<StudyPodcastEntity> findTop30ByStudentUserIdOrderByCreatedAtDesc(String studentUserId);

    Optional<StudyPodcastEntity> findByIdAndStudentUserId(Long id, String studentUserId);
}
