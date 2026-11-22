package org.example.learningprojectserver.strategy.message;

import org.example.learningprojectserver.entities.UserEntity;
import org.example.learningprojectserver.repository.ParentRepository;

import java.util.List;

/** Parents of every student in the sender's school. */
public class AllParentsStrategy implements MessageRecipientStrategy {

    private final ParentRepository parentRepository;

    public AllParentsStrategy(ParentRepository parentRepository) {
        this.parentRepository = parentRepository;
    }

    @Override
    public List<UserEntity> getRecipients(String senderId) {
        return parentRepository.findParentsOfSchoolForSender(senderId);
    }
}
