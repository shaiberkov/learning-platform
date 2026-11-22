package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.CalendarTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarTokenRepository extends JpaRepository<CalendarTokenEntity, Long> {

    CalendarTokenEntity findByUserId(String userId);

    CalendarTokenEntity findByToken(String token);
}
