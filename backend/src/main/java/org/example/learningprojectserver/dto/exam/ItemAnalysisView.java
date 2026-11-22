package org.example.learningprojectserver.dto.exam;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Classical item analysis for one exam.
 * difficulty = share of examinees who answered correctly (p).
 * discrimination = p(upper 27%) - p(lower 27%) by total score (D); null with fewer than 2 examinees.
 */
@Data
@NoArgsConstructor
public class ItemAnalysisView {
    private Long testId;
    private String title;
    private int examineeCount;
    private int groupSize;
    private List<Item> items = new ArrayList<>();

    @Data
    @NoArgsConstructor
    public static class Item {
        private Long questionId;
        private int position;
        private String text;
        private String kind;
        private int answeredCount;
        private int correctCount;
        private Double difficulty;
        private Double discrimination;
        private List<String> flags = new ArrayList<>();
    }
}
