package org.example.learningprojectserver.service.quest;

import org.example.learningprojectserver.entities.QuestionEntity;
import org.example.learningprojectserver.service.MathQuestion.MathQuestionFactory;
import org.springframework.stereotype.Component;

/** Questions for quest chapters, from the existing math generators (a bean so tests can replace it). */
@Component
public class QuestQuestionSource {

    private final MathQuestionFactory factory = new MathQuestionFactory();

    public QuestionEntity generate(String subTopic, int difficulty) {
        return factory.getQuestionGenerator("מתמטיקה", subTopic).generateQuestion(difficulty);
    }
}
