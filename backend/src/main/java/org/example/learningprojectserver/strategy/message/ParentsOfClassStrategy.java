package org.example.learningprojectserver.strategy.message;

import org.example.learningprojectserver.entities.UserEntity;
import org.example.learningprojectserver.repository.ParentRepository;

import java.util.List;

/** Parents of the students in one class of the sender's school. */
public class ParentsOfClassStrategy implements MessageRecipientStrategy {

    private final ParentRepository parentRepository;
    private final String className;

    public ParentsOfClassStrategy(ParentRepository parentRepository, String className) {
        this.parentRepository = parentRepository;
        this.className = className;
    }

    @Override
    public List<UserEntity> getRecipients(String senderId) {
        return parentRepository.findParentsOfClassForSender(className, senderId);
    }
}
