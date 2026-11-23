package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.PushSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, Long> {

    List<PushSubscriptionEntity> findByUserId(String userId);

    List<PushSubscriptionEntity> findByEndpoint(String endpoint);
}
