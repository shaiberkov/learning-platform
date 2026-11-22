package org.example.learningprojectserver.service.exam;

import org.example.learningprojectserver.dto.exam.ItemAnalysisView;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItemAnalysisCalculatorTest {

    private static final List<ItemAnalysisCalculator.Question> QUESTIONS = List.of(
            new ItemAnalysisCalculator.Question(1L, 1, "good", "NUMERIC"),
            new ItemAnalysisCalculator.Question(2L, 2, "easy", "NUMERIC"),
            new ItemAnalysisCalculator.Question(3L, 3, "reversed", "NUMERIC"));

    /** Ten examinees with scores 100, 90, ..., 10. */
    private static List<ItemAnalysisCalculator.Row> rows() {
        List<ItemAnalysisCalculator.Row> rows = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            long attempt = i + 1;
            int score = 100 - i * 10;
            boolean strong = i < 5;
            rows.add(new ItemAnalysisCalculator.Row(attempt, score, 1L, strong));
            rows.add(new ItemAnalysisCalculator.Row(attempt, score, 2L, true));
            rows.add(new ItemAnalysisCalculator.Row(attempt, score, 3L, !strong));
        }
        return rows;
    }

    @Test
    void groupSizeIs27PercentRounded() {
        assertEquals(0, ItemAnalysisCalculator.groupSize(1));
        assertEquals(1, ItemAnalysisCalculator.groupSize(2));
        assertEquals(3, ItemAnalysisCalculator.groupSize(10));
        assertEquals(27, ItemAnalysisCalculator.groupSize(100));
    }

    @Test
    void computesDifficultyAndDiscrimination() {
        List<ItemAnalysisView.Item> items = ItemAnalysisCalculator.analyse(QUESTIONS, rows());

        ItemAnalysisView.Item good = items.get(0);
        assertEquals(0.5, good.getDifficulty());
        assertEquals(1.0, good.getDiscrimination());
        assertTrue(good.getFlags().isEmpty());

        ItemAnalysisView.Item easy = items.get(1);
        assertEquals(1.0, easy.getDifficulty());
        assertEquals(0.0, easy.getDiscrimination());
        assertTrue(easy.getFlags().contains(ItemAnalysisCalculator.FLAG_TOO_EASY));
        assertTrue(easy.getFlags().contains(ItemAnalysisCalculator.FLAG_WEAK));

        ItemAnalysisView.Item reversed = items.get(2);
        assertEquals(-1.0, reversed.getDiscrimination());
        assertTrue(reversed.getFlags().contains(ItemAnalysisCalculator.FLAG_NEGATIVE));
    }

    @Test
    void noAttemptsGivesEmptyStatistics() {
        List<ItemAnalysisView.Item> items = ItemAnalysisCalculator.analyse(QUESTIONS, List.of());
        assertEquals(3, items.size());
        assertNull(items.get(0).getDifficulty());
        assertNull(items.get(0).getDiscrimination());
        assertTrue(items.get(0).getFlags().isEmpty());
    }
}
