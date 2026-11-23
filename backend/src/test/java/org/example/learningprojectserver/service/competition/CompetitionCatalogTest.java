package org.example.learningprojectserver.service.competition;

import org.example.learningprojectserver.entities.QuestionEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompetitionCatalogTest {

    @Test
    void ignoresDirectionMarksAndWhitespace() {
        assertTrue(CompetitionCatalog.answersMatch("(3, -2)", "(‎3, ‎-2)"));
        assertTrue(CompetitionCatalog.answersMatch("X = 3", "x=3"));
        assertFalse(CompetitionCatalog.answersMatch("(3, 2)", "(‎3, ‎-2)"));
    }

    @Test
    void everyCatalogSubTopicAcceptsItsOwnAnswerTypedWithoutMarks() {
        CompetitionCatalog.topics().values().stream().flatMap(java.util.List::stream).forEach(subTopic -> {
            for (int level = 1; level <= 5; level++) {
                QuestionEntity q = CompetitionCatalog.generate(subTopic, level);
                String typed = q.getAnswer().replaceAll("[\\u200E\\u200F\\u202A-\\u202E\\u2066-\\u2069]", "");
                assertTrue(CompetitionCatalog.answersMatch(typed, q.getAnswer()), subTopic + " level " + level);
            }
        });
    }

    @Test
    void derivativesAreNotOffered() {
        assertFalse(CompetitionCatalog.contains("ניגזרות"));
        assertFalse(CompetitionCatalog.forGrade(12).contains("ניגזרות"));
    }
}
