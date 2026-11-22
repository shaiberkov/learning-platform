package org.example.learningprojectserver.entities;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("TEST")
public class TestQuestionEntity extends QuestionEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id")
    private TestEntity test;

    // Exam question types (null kind = legacy generated question graded by exact text match).
    private String kind;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(columnDefinition = "TEXT")
    private String optionsJson;

    @Column(columnDefinition = "TEXT")
    private String answerKeyJson;

    private Double tolerance;
    private Integer points;
    private Integer position;

    public TestQuestionEntity() {
        super();
    }

    public TestEntity getTest() {
        return test;
    }

    public void setTest(TestEntity test) {
        this.test = test;
    }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getOptionsJson() { return optionsJson; }
    public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }

    public String getAnswerKeyJson() { return answerKeyJson; }
    public void setAnswerKeyJson(String answerKeyJson) { this.answerKeyJson = answerKeyJson; }

    public Double getTolerance() { return tolerance; }
    public void setTolerance(Double tolerance) { this.tolerance = tolerance; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    /** Full question text: the authored prompt when present, else the generated text. */
    public String getDisplayText() {
        return prompt != null && !prompt.isBlank() ? prompt : getQuestionText();
    }

    public int getPointsOrDefault() {
        return points == null || points <= 0 ? 1 : points;
    }
}
