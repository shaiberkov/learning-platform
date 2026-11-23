package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.MessageAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachmentEntity, Long> {
}
