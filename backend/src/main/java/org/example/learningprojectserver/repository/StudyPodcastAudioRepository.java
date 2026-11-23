package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.StudyPodcastAudioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyPodcastAudioRepository extends JpaRepository<StudyPodcastAudioEntity, Long> {
}
