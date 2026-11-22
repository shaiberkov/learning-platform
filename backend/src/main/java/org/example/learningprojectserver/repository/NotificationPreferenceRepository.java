package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.NotificationPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreferenceEntity, Long> {

    NotificationPreferenceEntity findByUserId(String userId);

    List<NotificationPreferenceEntity> findByUserIdIn(Collection<String> userIds);
}
