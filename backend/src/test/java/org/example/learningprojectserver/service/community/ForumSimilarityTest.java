package org.example.learningprojectserver.service.community;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ForumSimilarityTest {

    @Test
    void stopWordsAndShortWordsAreIgnored() {
        assertEquals(java.util.Set.of("משוואה"), ForumSimilarity.words("איך את משוואה ? x"));
    }

    @Test
    void hebrewPrefixLetterStillMatches() {
        int score = ForumSimilarity.score("המשוואה", null, "משוואה ריבועית", null, null, null);
        assertEquals(100, score);
    }

    @Test
    void sameTopicAddsBonusOnlyWhenWordsMatch() {
        int base = ForumSimilarity.score("שברים מכנה", null, "חיבור שברים", null, null, "שברים");
        int withTopic = ForumSimilarity.score("שברים מכנה", "שברים", "חיבור שברים", null, null, "שברים");
        assertEquals(base + 20, withTopic);
        assertEquals(0, ForumSimilarity.score("גאומטריה", "שברים", "חיבור שברים", null, null, "שברים"));
    }

    @Test
    void emptyQueryScoresZero() {
        assertEquals(0, ForumSimilarity.score("  ", null, "כל דבר", null, null, null));
    }
}
