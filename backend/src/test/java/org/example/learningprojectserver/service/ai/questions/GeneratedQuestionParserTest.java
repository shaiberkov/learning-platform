package org.example.learningprojectserver.service.ai.questions;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedQuestionParserTest {

    @Test
    void parsesQuestionsAndSkipsIncompleteOnes() {
        String json = """
                {"questions":[
                  {"questionText":"פתרו: x^2-5x+6=0","answer":"2,3","explanation":"פירוק לגורמים","subTopic":"משוואה ריבועית","difficulty":"בינוני"},
                  {"questionText":"שאלה בלי תשובה","answer":""},
                  {"questionText":"כמה זה 7*8?","answer":"56"}
                ]}""";

        List<GeneratedQuestionParser.GeneratedQuestion> questions = GeneratedQuestionParser.parse(json, 10);

        assertEquals(2, questions.size());
        assertEquals("2,3", questions.get(0).answer());
        assertEquals("", questions.get(1).explanation());
    }

    @Test
    void respectsTheRequestedCountAndCodeFences() {
        String json = "```json\n{\"questions\":[{\"questionText\":\"a\",\"answer\":\"1\"},{\"questionText\":\"b\",\"answer\":\"2\"}]}\n```";
        assertEquals(1, GeneratedQuestionParser.parse(json, 1).size());
    }

    @Test
    void invalidJsonGivesNothing() {
        assertTrue(GeneratedQuestionParser.parse("not json", 5).isEmpty());
        assertTrue(GeneratedQuestionParser.parse(null, 5).isEmpty());
    }
}
